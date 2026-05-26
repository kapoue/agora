#!/usr/bin/env python3
"""
Script de préparation de release Agora.
Étape 1 : Génère les questions QCM via Mistral (Gemini en fallback si quota dépassé).
Étape 2 : Récupère les URLs d'images par thème×difficulté (Unsplash → Pexels → Pixabay)
          et les injecte dans les fichiers JSON de questions (champ "imageUrl" par question).

Les deux étapes sont indépendantes et peuvent être lancées séparément :
  - Questions : toutes les 2-3 semaines (contenu)
  - Images    : tous les 2-3 mois (URLs Unsplash périment)

Les clés API sont lues depuis local.properties à la racine du projet agora.
Elles ne transitent jamais dans le code source ni dans l'APK.

Usage :
    # Questions uniquement (pas d'images)
    python3 prepare_release.py --no-images               # Génère les fichiers manquants
    python3 prepare_release.py --no-images --force       # Régénère tout
    python3 prepare_release.py --no-images --theme HISTOIRE          # Un seul thème
    python3 prepare_release.py --no-images --theme HISTOIRE --difficulty DEBUTANT

    # Images uniquement (ne touche pas aux questions)
    python3 prepare_release.py --images-only             # Tous les thèmes
    python3 prepare_release.py --images-only --theme HISTOIRE        # Un seul thème

    # Les deux en une passe (comportement par défaut)
    python3 prepare_release.py                           # Manquants + images
    python3 prepare_release.py --force                   # Tout régénérer

    # Options communes
    python3 prepare_release.py --no-git                  # Sans commit/tag git
"""

import argparse
import json
import os
import re
import subprocess
import time
import random

import requests
from google import genai
from mistralai import Mistral as _MistralClient

# ─── WRAPPERS FOURNISSEURS LLM ───────────────────────────────────────────────
class MistralProvider:
    def __init__(self, key: str):
        self._client = _MistralClient(api_key=key)
        self.label = "Mistral"

    def generate(self, prompt: str) -> str:
        resp = self._client.chat.complete(
            model="mistral-small-latest",
            messages=[{"role": "user", "content": prompt}],
        )
        return resp.choices[0].message.content


class GeminiProvider:
    def __init__(self, key: str, index: int):
        self._client = genai.Client(api_key=key)
        self.label = f"Gemini #{index}"

    def generate(self, prompt: str) -> str:
        resp = self._client.models.generate_content(model="gemini-2.5-flash", contents=prompt)
        return resp.text


# ─── CHEMINS ──────────────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.join(SCRIPT_DIR, "..")
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "questions")
LOCAL_PROPERTIES = os.path.join(PROJECT_ROOT, "local.properties")
BUILD_GRADLE = os.path.join(PROJECT_ROOT, "app", "build.gradle.kts")

# ─── CONFIGURATION QUESTIONS ──────────────────────────────────────────────────
QUESTIONS_PER_COMBO = 60
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
    "GASTRONOMIE":   ("gourmet food cuisine chef",       "Gastronomie (cuisine du monde, techniques culinaires, chefs étoilés, vins, fromages, gastronomie française et internationale, recettes emblématiques)"),
    "JEUX_VIDEO":    ("video games controller gaming",   "Jeux vidéo (jeux cultes, consoles, studios de développement, personnages iconiques, histoire du jeu vidéo, records et récompenses)"),
}

DIFFICULTIES = {
    "DEBUTANT":      "débutant — questions simples, connues du grand public, niveau fin de collège. Les réponses sont évidentes pour quelqu'un de curieux.",
    "MOYEN":         "intermédiaire — questions de culture générale solide, niveau lycée ou adulte cultivé. Il faut réfléchir.",
    "EXPERT":        "expert — questions pointues, nécessitant une vraie expertise ou une culture très approfondie sur le sujet.",
}

# Pages Unsplash/Pexels par difficulté pour garantir des pools distincts
DIFFICULTY_PAGE = {"DEBUTANT": 1, "MOYEN": 2, "EXPERT": 3}

PROMPT_TEMPLATE = """Tu es un expert en création de quiz éducatifs. Génère exactement {n} questions QCM uniques sur le thème suivant :

THÈME : {theme_desc}
DIFFICULTÉ : {difficulty_desc}

RÈGLES STRICTES :
- Toutes les questions et réponses doivent être rédigées en français
- Chaque question a exactement 1 bonne réponse et 3 mauvaises réponses
- Les mauvaises réponses doivent être plausibles mais clairement incorrectes
- Respecte scrupuleusement le niveau de difficulté
- Ne mets pas de numérotation dans les questions
- Les réponses ne doivent pas dépasser 80 caractères chacune

RÈGLES ANTI-DOUBLONS (OBLIGATOIRES) :
- Chaque question doit porter sur un sujet, événement, personnage, œuvre ou concept STRICTEMENT DIFFÉRENT des autres
- Il est INTERDIT de poser deux questions sur le même sujet, même formulées différemment
- Il est INTERDIT de mentionner la même personne, œuvre ou lieu dans plus d'une question
- Varie au maximum les angles : dates, lieux, personnes, œuvres, définitions, records, anecdotes, étymologies...
- Si le thème est pointu, élargis aux sous-domaines connexes plutôt que de répéter les mêmes sujets

EXPLICATION (OBLIGATOIRE) :
- Chaque question doit inclure un champ "explanation" : une phrase courte (1-2 phrases max) qui explique pourquoi la bonne réponse est correcte
- L'explication doit apporter un contexte ou un fait mémorisable, pas juste répéter la réponse
- Longueur : 20 à 120 caractères

Réponds UNIQUEMENT avec un tableau JSON valide, sans texte avant ou après, sans balises markdown :
[
  {{
    "question": "Texte de la question ?",
    "correct_answer": "La bonne réponse",
    "incorrect_answers": ["Mauvaise réponse 1", "Mauvaise réponse 2", "Mauvaise réponse 3"],
    "explanation": "Courte explication mémorisable de la bonne réponse."
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
            entry = {
                "question": str(q["question"]).strip(),
                "correct_answer": str(q["correct_answer"]).strip(),
                "incorrect_answers": [str(a).strip() for a in q["incorrect_answers"]],
            }
            if "explanation" in q and isinstance(q["explanation"], str) and q["explanation"].strip():
                entry["explanation"] = str(q["explanation"]).strip()
            valid.append(entry)
    return valid


def generate_questions_for(providers: list, theme: str, difficulty: str) -> list:
    _, theme_desc = THEMES[theme]
    prompt = PROMPT_TEMPLATE.format(
        n=QUESTIONS_PER_COMBO,
        theme_desc=theme_desc,
        difficulty_desc=DIFFICULTIES[difficulty],
    )
    provider_index = 0
    retry_count = 0
    attempt = 0
    while provider_index < len(providers):
        attempt += 1
        provider = providers[provider_index]
        try:
            print(f"    Tentative {attempt} ({provider.label})...", end=" ", flush=True)
            text = provider.generate(prompt)
            questions = validate_questions(extract_json(text))
            if len(questions) < 10:
                raise ValueError(f"Seulement {len(questions)} questions valides")
            print(f"✓ {len(questions)} questions")
            return questions
        except Exception as e:
            err_str = str(e)
            if "429" in err_str or "403" in err_str or "401" in err_str:
                reason = "quota/limite" if "429" in err_str else "clé invalide"
                print(f"✗ {reason} ({provider.label}), rotation...")
                provider_index += 1
                retry_count = 0
                time.sleep(2)
            else:
                print(f"✗ {e}")
                retry_count += 1
                if retry_count < MAX_RETRIES:
                    wait = DELAY_BETWEEN_CALLS * min(retry_count, 3)
                    print(f"    Attente {wait}s...")
                    time.sleep(wait)
                else:
                    provider_index += 1
                    retry_count = 0
    raise RuntimeError(f"Échec pour {theme}/{difficulty}")


# ─── RÉCUPÉRATION IMAGES ──────────────────────────────────────────────────────
def fetch_image_urls(query: str, count: int, page: int, api_keys: dict) -> list:
    """Accumule Unsplash (multi-pages) → complète avec Pexels → Pixabay."""
    urls: list = []
    unsplash_key = api_keys.get("UNSPLASH_API_KEY", "")
    # Unsplash : plan gratuit limite à ~10 résultats par requête, donc on pagine
    for p in [page, page + 1, page + 2]:
        if len(urls) >= count:
            break
        batch = _try_unsplash(query, 30, p, unsplash_key)
        urls += [u for u in batch if u not in urls]
    # Compléter avec Pexels si pas assez
    if len(urls) < count:
        pexels = _try_pexels(query, count - len(urls), page, api_keys.get("PEXELS_API_KEY", ""))
        urls += [u for u in pexels if u not in urls]
    # Compléter avec Pixabay si toujours pas assez
    if len(urls) < count:
        pixabay = _try_pixabay(query, count - len(urls), page, api_keys.get("PIXABAY_API_KEY", ""))
        urls += [u for u in pixabay if u not in urls]
    random.shuffle(urls)
    return urls[:count]


def _try_unsplash(query: str, count: int, page: int, key: str) -> list:
    if not key:
        return []
    try:
        r = requests.get(
            "https://api.unsplash.com/search/photos",
            params={"query": query, "per_page": min(count, 30), "page": page},
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


# ─── MONTÉE DE VERSION PATCH ──────────────────────────────────────────────────
def bump_patch_version() -> tuple[str, str]:
    """
    Lit app/build.gradle.kts, incrémente versionCode et le patch (z) de versionName.
    Retourne (old_version, new_version).
    """
    with open(BUILD_GRADLE, encoding="utf-8") as f:
        content = f.read()

    # versionCode
    code_match = re.search(r'versionCode\s*=\s*(\d+)', content)
    if not code_match:
        raise RuntimeError("versionCode introuvable dans build.gradle.kts")
    old_code = int(code_match.group(1))
    new_code = old_code + 1

    # versionName x.y.z → x.y.(z+1)
    name_match = re.search(r'versionName\s*=\s*"(\d+)\.(\d+)\.(\d+)"', content)
    if not name_match:
        raise RuntimeError("versionName introuvable dans build.gradle.kts")
    major, minor, patch = name_match.group(1), name_match.group(2), name_match.group(3)
    old_version = f"{major}.{minor}.{patch}"
    new_version = f"{major}.{minor}.{int(patch) + 1}"

    content = content.replace(
        f"versionCode = {old_code}",
        f"versionCode = {new_code}"
    )
    content = content.replace(
        f'versionName = "{old_version}"',
        f'versionName = "{new_version}"'
    )

    with open(BUILD_GRADLE, "w", encoding="utf-8") as f:
        f.write(content)

    return old_version, new_version


def git_commit_and_tag(version: str) -> None:
    """git add -A → commit → tag vX.Y.Z → push origin main + tag."""
    cmds = [
        ["git", "add", "-A"],
        ["git", "commit", "-m", f"content: inject image URLs + bump version to {version}"],
        ["git", "tag", f"v{version}"],
        ["git", "push", "origin", "main"],
        ["git", "push", "origin", f"v{version}"],
    ]
    for cmd in cmds:
        print(f"    $ {' '.join(cmd)}")
        result = subprocess.run(cmd, cwd=PROJECT_ROOT, capture_output=True, text=True)
        if result.returncode != 0:
            print(f"    ⚠  {result.stderr.strip()}")
        else:
            if result.stdout.strip():
                print(f"    {result.stdout.strip()}")


# ─── MAIN ─────────────────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="Prépare la release Agora (questions + images)")
    parser.add_argument("--force",       action="store_true", help="Régénère questions et images même si déjà présents")
    parser.add_argument("--images-only", action="store_true", help="Rafraîchit uniquement les images (ne touche pas aux questions)")
    parser.add_argument("--no-images",   action="store_true", help="Génère uniquement les questions (pas d'images)")
    parser.add_argument("--theme",       type=str, help="Traite uniquement ce thème (ex: HISTOIRE)")
    parser.add_argument("--difficulty",  type=str, help="Traite uniquement cette difficulté (ex: DEBUTANT)")
    parser.add_argument("--no-git",      action="store_true", help="Ne fait pas le commit/tag git")
    args = parser.parse_args()

    if args.images_only and args.no_images:
        print("⚠  --images-only et --no-images sont incompatibles")
        return

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    api_keys = load_api_keys()

    themes      = [args.theme]      if args.theme      else list(THEMES.keys())
    difficulties = [args.difficulty] if args.difficulty else list(DIFFICULTIES.keys())
    total = len(themes) * len(difficulties)

    print(f"\n{'='*60}")
    mode = "images seules" if args.images_only else ("questions seules" if args.no_images else "questions + images")
    print(f"  AGORA — Préparation release")
    print(f"  {total} combinaisons | mode={mode} | force={args.force}")
    print(f"  Sortie : {os.path.abspath(OUTPUT_DIR)}")
    print(f"{'='*60}\n")

    # Providers LLM : Mistral en premier, Gemini en fallback (inutilisés si --images-only ou --no-images sans génération)
    if not args.images_only:
        providers = []
        mistral_key = api_keys.get("MISTRAL_API_KEY", "")
        if mistral_key:
            providers.append(MistralProvider(mistral_key))
        gemini_keys = [v for k, v in sorted(api_keys.items()) if k.startswith("GEMINI_API_KEY_")]
        for i, key in enumerate(gemini_keys, 1):
            providers.append(GeminiProvider(key, i))
        if not providers:
            print("⚠  Aucun fournisseur LLM configuré dans local.properties")
    else:
        providers = []

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
                        print(f"    Questions : génération LLM...")
                        questions_data = generate_questions_for(providers, theme, difficulty)
                        # Déduplication par texte exact
                        seen = set()
                        unique = []
                        for q in questions_data:
                            key = q["question"].strip().lower()
                            if key not in seen:
                                seen.add(key)
                                unique.append(q)
                        removed = len(questions_data) - len(unique)
                        if removed:
                            print(f"    Questions : {removed} doublon(s) supprimé(s)")
                        questions_data = unique
                        # Écriture temporaire sans images
                        with open(filepath, "w", encoding="utf-8") as f:
                            json.dump(questions_data, f, ensure_ascii=False, indent=2)
                        print(f"    Questions : {len(questions_data)} sauvegardées")
                        if total > 1:
                            time.sleep(DELAY_BETWEEN_CALLS)

                # ── Étape 2 : Images ────────────────────────────────────────
                if args.no_images:
                    print(f"    Images    : ignorées (--no-images)")
                    continue

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
        print(f"  Version non montée (erreurs présentes)")
    else:
        print(f"  ✓ Toutes les combinaisons traitées avec succès")
        # Montée de version patch
        try:
            old_v, new_v = bump_patch_version()
            print(f"  ✓ Version : {old_v} → {new_v}")
        except Exception as e:
            print(f"  ⚠  Impossible de monter la version : {e}")
            new_v = None

        # Git commit + tag
        if not args.no_git and new_v:
            print(f"\n  Git :")
            git_commit_and_tag(new_v)
        elif args.no_git:
            print(f"  (--no-git : commit/tag ignorés)")
    print(f"{'='*60}\n")


if __name__ == "__main__":
    main()
