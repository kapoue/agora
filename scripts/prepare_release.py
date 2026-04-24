#!/usr/bin/env python3
"""
Script de préparation de release Agora.
Étape 1 : Génère les questions QCM via Gemini (si manquantes ou --force).
Étape 2 : Récupère 30 URLs d'images par thème×difficulté (Unsplash → Pexels → Pixabay)
          et les injecte dans les fichiers JSON de questions (champ "imageUrl" par question).

Les clés API sont lues depuis local.properties à la racine du projet agora.
Elles ne transitent jamais dans le code source ni dans l'APK.

Usage :
    python prepare_release.py                    # Génère les manquants + toutes les images
    python prepare_release.py --force            # Régénère tout (questions + images)
    python prepare_release.py --images-only      # Rafraîchit uniquement les images
    python prepare_release.py --theme HISTOIRE   # Un seul thème
    python prepare_release.py --difficulty DEBUTANT  # Une seule difficulté
"""

import argparse
import json
import os
import re
import time
import random

import requests
from google import genai

# ─── CHEMINS ──────────────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.join(SCRIPT_DIR, "..")
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "questions")
LOCAL_PROPERTIES = os.path.join(PROJECT_ROOT, "local.properties")

# ─── CLÉS GEMINI ──────────────────────────────────────────────────────────────
GEMINI_API_KEYS = [
    "AIzaSyBEdKCxAGBhyLJLuy1cdcBXPF6nYvBjJkM",
    "AIzaSyDJgIsefbeDHRMIE_4hA1wYA3KziIo1qOU",
    "AIzaSyB7LiJXfI4yqFG_w-90dO34OMaJnJBUFMQ",
    "AIzaSyBFbXVZ62Zj62yQkRV2omtsU9USE7_AKqc",
    "AIzaSyC_ifMwwk-iLcCJcEHgWjt58RKtLk17K5k",
    "AIzaSyBwltFRQFXuzSuwEcdfGPnjKS6kHxGEhTI",
]

# ─── CONFIGURATION QUESTIONS ──────────────────────────────────────────────────
QUESTIONS_PER_COMBO = 30
DELAY_BETWEEN_CALLS = 5
MAX_RETRIES = 3

THEMES = {
    "HISTOIRE":      ("ancient history monument",        "Histoire (événements historiques mondiaux, personnages historiques, dates importantes, guerres, empires, révolutions)"),
    "GEOGRAPHIE":    ("world map geography landscape",   "Géographie (pays, capitales, fleuves, montagnes, mers, continents, records géographiques)"),
    "SCIENCES":      ("science laboratory nature",       "Sciences & Nature (biologie, physique, chimie, astronomie, médecine, environnement, inventions scientifiques)"),
    "CINEMA":        ("cinema film camera",              "Cinéma (films, réalisateurs, acteurs célèbres, prix cinématographiques, histoire du cinéma)"),
    "MUSIQUE":       ("music instruments concert",       "Musique (artistes, albums, instruments de musique, genres musicaux, histoire de la musique)"),
    "SPORT":         ("sport competition stadium",       "Sport (sports, champions, records sportifs, Jeux Olympiques, compétitions mondiales, règles sportives)"),
    "LITTERATURE":   ("books library literature",        "Littérature (auteurs, romans célèbres, poètes, prix littéraires, œuvres classiques et modernes)"),
    "INFORMATIQUE":  ("computer technology code",        "Informatique (programmation, histoire de l'informatique, entreprises tech, inventions numériques, internet)"),
    "ART":           ("painting art museum gallery",     "Art (peintres, sculpteurs, mouvements artistiques, musées, œuvres d'art célèbres, architecture)"),
    "MATHEMATIQUES": ("mathematics formula blackboard",  "Mathématiques (théorèmes, géométrie, mathématiciens célèbres, concepts mathématiques, statistiques)"),
    "MYTHOLOGIE":    ("mythology ancient gods temple",   "Mythologie (dieux grecs, romains, nordiques, égyptiens, héros mythologiques, créatures légendaires)"),
    "ANIMAUX":       ("wild animals nature wildlife",    "Animaux (espèces animales, comportements, habitats naturels, records animaliers, classification zoologique)"),
    "VEHICULES":     ("vehicles cars aviation transport","Véhicules (voitures, avions, bateaux, trains, histoire des transports, records de vitesse)"),
}

DIFFICULTIES = {
    "DEBUTANT":      "débutant — questions simples, connues du grand public, niveau fin de collège. Les réponses sont évidentes pour quelqu'un de curieux.",
    "INTERMEDIAIRE": "intermédiaire — questions de culture générale solide, niveau lycée ou adulte cultivé. Il faut réfléchir.",
    "EXPERT":        "expert — questions pointues, nécessitant une vraie expertise ou une culture très approfondie sur le sujet.",
}

# Pages Unsplash/Pexels par difficulté pour garantir des pools distincts
DIFFICULTY_PAGE = {"DEBUTANT": 1, "INTERMEDIAIRE": 2, "EXPERT": 3}

PROMPT_TEMPLATE = """Tu es un expert en création de quiz éducatifs. Génère exactement {n} questions QCM uniques sur le thème suivant :

THÈME : {theme_desc}
DIFFICULTÉ : {difficulty_desc}

RÈGLES STRICTES :
- Toutes les questions et réponses doivent être rédigées en français
- Chaque question a exactement 1 bonne réponse et 3 mauvaises réponses
- Les mauvaises réponses doivent être plausibles mais clairement incorrectes
- Les questions doivent être variées (pas de répétitions, pas de formulations similaires)
- Respecte scrupuleusement le niveau de difficulté
- Ne mets pas de numérotation dans les questions
- Les réponses ne doivent pas dépasser 80 caractères chacune

Réponds UNIQUEMENT avec un tableau JSON valide, sans texte avant ou après, sans balises markdown :
[
  {{
    "question": "Texte de la question ?",
    "correct_answer": "La bonne réponse",
    "incorrect_answers": ["Mauvaise réponse 1", "Mauvaise réponse 2", "Mauvaise réponse 3"]
  }}
]"""


# ─── LECTURE local.properties ─────────────────────────────────────────────────
def load_api_keys() -> dict:
    keys = {}
    if not os.path.exists(LOCAL_PROPERTIES):
        print(f"⚠  local.properties introuvable : {LOCAL_PROPERTIES}")
        return keys
    with open(LOCAL_PROPERTIES, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if "=" in line and not line.startswith("#"):
                k, _, v = line.partition("=")
                keys[k.strip()] = v.strip()
    return keys


# ─── GÉNÉRATION QUESTIONS ─────────────────────────────────────────────────────
def extract_json(text: str) -> list:
    text = re.sub(r"```json\s*", "", text)
    text = re.sub(r"```\s*", "", text)
    text = text.strip()
    start = text.find("[")
    end = text.rfind("]")
    if start == -1 or end == -1:
        raise ValueError("Aucun tableau JSON trouvé")
    return json.loads(text[start:end + 1])


def validate_questions(questions: list) -> list:
    valid = []
    for q in questions:
        if (
            isinstance(q, dict)
            and "question" in q
            and "correct_answer" in q
            and "incorrect_answers" in q
            and isinstance(q["incorrect_answers"], list)
            and len(q["incorrect_answers"]) == 3
            and all(isinstance(a, str) for a in q["incorrect_answers"])
        ):
            valid.append({
                "question": str(q["question"]).strip(),
                "correct_answer": str(q["correct_answer"]).strip(),
                "incorrect_answers": [str(a).strip() for a in q["incorrect_answers"]],
            })
    return valid


def generate_questions_for(clients, theme: str, difficulty: str) -> list:
    _, theme_desc = THEMES[theme]
    prompt = PROMPT_TEMPLATE.format(
        n=QUESTIONS_PER_COMBO,
        theme_desc=theme_desc,
        difficulty_desc=DIFFICULTIES[difficulty],
    )
    client_index = 0
    for attempt in range(1, MAX_RETRIES * len(clients) + 1):
        client = clients[client_index]
        try:
            print(f"    Tentative {attempt} (clé {client_index + 1}/{len(clients)})...", end=" ", flush=True)
            response = client.models.generate_content(model="gemini-2.5-flash", contents=prompt)
            questions = validate_questions(extract_json(response.text))
            if len(questions) < 10:
                raise ValueError(f"Seulement {len(questions)} questions valides")
            print(f"✓ {len(questions)} questions")
            return questions
        except Exception as e:
            err_str = str(e)
            if "429" in err_str and "GenerateRequestsPerDayPerProjectPerModel" in err_str:
                print(f"✗ quota épuisé (clé {client_index + 1})")
                client_index = (client_index + 1) % len(clients)
                if client_index == 0:
                    break
                time.sleep(2)
            else:
                print(f"✗ {e}")
                if attempt < MAX_RETRIES * len(clients):
                    wait = DELAY_BETWEEN_CALLS * min(attempt, 3)
                    print(f"    Attente {wait}s...")
                    time.sleep(wait)
    raise RuntimeError(f"Échec pour {theme}/{difficulty}")


# ─── RÉCUPÉRATION IMAGES ──────────────────────────────────────────────────────
def fetch_image_urls(query: str, count: int, page: int, api_keys: dict) -> list:
    """Essaie Unsplash → Pexels → Pixabay. Retourne une liste mélangée de `count` URLs."""
    urls = _try_unsplash(query, count, page, api_keys.get("UNSPLASH_API_KEY", ""))
    if not urls:
        urls = _try_pexels(query, count, page, api_keys.get("PEXELS_API_KEY", ""))
    if not urls:
        urls = _try_pixabay(query, count, page, api_keys.get("PIXABAY_API_KEY", ""))
    random.shuffle(urls)
    return urls[:count]


def _try_unsplash(query: str, count: int, page: int, key: str) -> list:
    if not key:
        return []
    try:
        r = requests.get(
            "https://api.unsplash.com/search/photos",
            params={"query": query, "per_page": count, "page": page},
            headers={"Authorization": f"Client-ID {key}"},
            timeout=15,
        )
        if r.status_code == 200:
            urls = [p["urls"]["regular"] for p in r.json().get("results", [])]
            if urls:
                print(f"      Unsplash OK : {len(urls)} URLs")
                return urls
        else:
            print(f"      Unsplash {r.status_code} → fallback Pexels")
    except Exception as e:
        print(f"      Unsplash erreur : {e} → fallback Pexels")
    return []


def _try_pexels(query: str, count: int, page: int, key: str) -> list:
    if not key:
        return []
    try:
        r = requests.get(
            "https://api.pexels.com/v1/search",
            params={"query": query, "per_page": count, "page": page},
            headers={"Authorization": key},
            timeout=15,
        )
        if r.status_code == 200:
            urls = [p["src"]["large2x"] for p in r.json().get("photos", [])]
            if urls:
                print(f"      Pexels OK : {len(urls)} URLs")
                return urls
        else:
            print(f"      Pexels {r.status_code} → fallback Pixabay")
    except Exception as e:
        print(f"      Pexels erreur : {e} → fallback Pixabay")
    return []


def _try_pixabay(query: str, count: int, page: int, key: str) -> list:
    if not key:
        return []
    try:
        r = requests.get(
            "https://pixabay.com/api/",
            params={"key": key, "q": query, "per_page": count, "page": page, "image_type": "photo"},
            timeout=15,
        )
        if r.status_code == 200:
            urls = [h["largeImageURL"] for h in r.json().get("hits", [])]
            if urls:
                print(f"      Pixabay OK : {len(urls)} URLs")
                return urls
        else:
            print(f"      Pixabay {r.status_code}")
    except Exception as e:
        print(f"      Pixabay erreur : {e}")
    return []


# ─── INJECTION IMAGES DANS JSON ───────────────────────────────────────────────
def inject_images(filepath: str, urls: list) -> None:
    """
    Lit le fichier JSON, assigne urls[i] à questions[i] (par positionInPool ou index),
    et réécrit le fichier.
    """
    with open(filepath, encoding="utf-8") as f:
        questions = json.load(f)

    if not isinstance(questions, list):
        print(f"      ⚠  Format inattendu dans {os.path.basename(filepath)}")
        return

    for i, q in enumerate(questions):
        url = urls[i] if i < len(urls) else None
        q["imageUrl"] = url

    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(questions, f, ensure_ascii=False, indent=2)


# ─── MAIN ─────────────────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="Prépare la release Agora (questions + images)")
    parser.add_argument("--force",       action="store_true", help="Régénère questions et images même si déjà présents")
    parser.add_argument("--images-only", action="store_true", help="Rafraîchit uniquement les images")
    parser.add_argument("--theme",       type=str, help="Traite uniquement ce thème (ex: HISTOIRE)")
    parser.add_argument("--difficulty",  type=str, help="Traite uniquement cette difficulté (ex: DEBUTANT)")
    args = parser.parse_args()

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    api_keys = load_api_keys()

    themes      = [args.theme]      if args.theme      else list(THEMES.keys())
    difficulties = [args.difficulty] if args.difficulty else list(DIFFICULTIES.keys())
    total = len(themes) * len(difficulties)

    print(f"\n{'='*60}")
    print(f"  AGORA — Préparation release")
    print(f"  {total} combinaisons | images-only={args.images_only} | force={args.force}")
    print(f"  Sortie : {os.path.abspath(OUTPUT_DIR)}")
    print(f"{'='*60}\n")

    # Clients Gemini (inutilisés si --images-only)
    clients = [] if args.images_only else [genai.Client(api_key=k) for k in GEMINI_API_KEYS]

    errors = []

    for theme in themes:
        if theme not in THEMES:
            print(f"⚠  Thème inconnu : {theme}")
            continue

        image_query, _ = THEMES[theme]

        for difficulty in difficulties:
            if difficulty not in DIFFICULTIES:
                print(f"⚠  Difficulté inconnue : {difficulty}")
                continue

            filepath = os.path.join(OUTPUT_DIR, f"{theme}_{difficulty}.json")
            label = f"{theme} / {difficulty}"
            page = DIFFICULTY_PAGE.get(difficulty, 1)

            print(f"  ▶ {label}")

            try:
                # ── Étape 1 : Questions ──────────────────────────────────────
                if not args.images_only:
                    if os.path.exists(filepath) and not args.force:
                        print(f"    Questions : déjà présentes (--force pour régénérer)")
                    else:
                        print(f"    Questions : génération via Gemini...")
                        questions_data = generate_questions_for(clients, theme, difficulty)
                        # Écriture temporaire sans images
                        with open(filepath, "w", encoding="utf-8") as f:
                            json.dump(questions_data, f, ensure_ascii=False, indent=2)
                        print(f"    Questions : {len(questions_data)} sauvegardées")
                        if total > 1:
                            time.sleep(DELAY_BETWEEN_CALLS)

                # ── Étape 2 : Images ────────────────────────────────────────
                if not os.path.exists(filepath):
                    print(f"    ⚠  Fichier JSON absent, images ignorées")
                    continue

                print(f"    Images    : fetch {QUESTIONS_PER_COMBO} URLs (page {page})...")
                urls = fetch_image_urls(image_query, QUESTIONS_PER_COMBO, page, api_keys)

                if not urls:
                    print(f"    ⚠  Aucune URL récupérée — imageUrl sera null")

                inject_images(filepath, urls)
                print(f"    Images    : {len(urls)} URLs injectées dans {os.path.basename(filepath)}")

            except Exception as e:
                errors.append((label, str(e)))
                print(f"    ✗ ERREUR : {e}")

    print(f"\n{'='*60}")
    if errors:
        print(f"  ✗ {len(errors)} erreur(s) :")
        for lbl, err in errors:
            print(f"    - {lbl} : {err}")
    else:
        print(f"  ✓ Toutes les combinaisons traitées avec succès")
    print(f"{'='*60}\n")


if __name__ == "__main__":
    main()
