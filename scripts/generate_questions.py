#!/usr/bin/env python3
"""
Script de génération des questions Agora via l'API Gemini.
Génère 30 questions QCM en français pour chaque combinaison thème × difficulté.
Sortie : fichiers JSON dans app/src/main/assets/questions/

Usage :
    python generate_questions.py                    # Génère tous les fichiers manquants
    python generate_questions.py --force            # Régénère tous les fichiers
    python generate_questions.py --theme HISTOIRE   # Un seul thème
"""

from google import genai
from google.genai import types
import json
import time
import os
import re
import argparse

# ─── CONFIGURATION ────────────────────────────────────────────────────────────
QUESTIONS_PER_COMBO = 30
DELAY_BETWEEN_CALLS = 5  # secondes (limite Gemini free tier : 15 req/min)
MAX_RETRIES = 3

OUTPUT_DIR = os.path.join(
    os.path.dirname(__file__), "..", "app", "src", "main", "assets", "questions"
)
LOCAL_PROPERTIES = os.path.join(os.path.dirname(__file__), "..", "local.properties")


def load_gemini_keys() -> list:
    """Lit les clés GEMINI_API_KEY_x depuis local.properties."""
    keys = {}
    if not os.path.exists(LOCAL_PROPERTIES):
        raise FileNotFoundError(f"local.properties introuvable : {LOCAL_PROPERTIES}")
    with open(LOCAL_PROPERTIES, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if "=" in line and not line.startswith("#"):
                k, _, v = line.partition("=")
                keys[k.strip()] = v.strip()
    gemini_keys = [v for k, v in sorted(keys.items()) if k.startswith("GEMINI_API_KEY_")]
    if not gemini_keys:
        raise ValueError("Aucune clé GEMINI_API_KEY_x trouvée dans local.properties")
    return gemini_keys

# ─── THÈMES ───────────────────────────────────────────────────────────────────
THEMES = {
    "HISTOIRE": "Histoire (événements historiques mondiaux, personnages historiques, dates importantes, guerres, empires, révolutions)",
    "GEOGRAPHIE": "Géographie (pays, capitales, fleuves, montagnes, mers, continents, records géographiques)",
    "SCIENCES": "Sciences & Nature (biologie, physique, chimie, astronomie, médecine, environnement, inventions scientifiques)",
    "CINEMA": "Cinéma (films, réalisateurs, acteurs célèbres, prix cinématographiques, histoire du cinéma)",
    "MUSIQUE": "Musique (artistes, albums, instruments de musique, genres musicaux, histoire de la musique)",
    "SPORT": "Sport (sports, champions, records sportifs, Jeux Olympiques, compétitions mondiales, règles sportives)",
    "LITTERATURE": "Littérature (auteurs, romans célèbres, poètes, prix littéraires, œuvres classiques et modernes)",
    "INFORMATIQUE": "Informatique (programmation, histoire de l'informatique, entreprises tech, inventions numériques, internet)",
    "ART": "Art (peintres, sculpteurs, mouvements artistiques, musées, œuvres d'art célèbres, architecture)",
    "MATHEMATIQUES": "Mathématiques (théorèmes, géométrie, mathématiciens célèbres, concepts mathématiques, statistiques)",
    "MYTHOLOGIE": "Mythologie (dieux grecs, romains, nordiques, égyptiens, héros mythologiques, créatures légendaires)",
    "ANIMAUX": "Animaux (espèces animales, comportements, habitats naturels, records animaliers, classification zoologique)",
    "VEHICULES": "Véhicules (voitures, avions, bateaux, trains, histoire des transports, records de vitesse)",
}

# ─── DIFFICULTÉS ──────────────────────────────────────────────────────────────
DIFFICULTIES = {
    "DEBUTANT": "débutant — questions simples, connues du grand public, niveau fin de collège. Les réponses sont évidentes pour quelqu'un de curieux.",
    "MOYEN": "intermédiaire — questions de culture générale solide, niveau lycée ou adulte cultivé. Il faut réfléchir.",
    "EXPERT": "expert — questions pointues, nécessitant une vraie expertise ou une culture très approfondie sur le sujet.",
}

# ─── PROMPT ───────────────────────────────────────────────────────────────────
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


# ─── EXTRACTION JSON ──────────────────────────────────────────────────────────
def extract_json(text: str) -> list:
    """Extrait le tableau JSON de la réponse Gemini (qui peut avoir des balises markdown)."""
    # Supprimer les balises markdown si présentes
    text = re.sub(r"```json\s*", "", text)
    text = re.sub(r"```\s*", "", text)
    text = text.strip()

    # Trouver le premier '[' et le dernier ']'
    start = text.find("[")
    end = text.rfind("]")
    if start == -1 or end == -1:
        raise ValueError("Aucun tableau JSON trouvé dans la réponse")

    json_str = text[start : end + 1]
    return json.loads(json_str)


def validate_questions(questions: list) -> list:
    """Valide et nettoie les questions."""
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
            valid.append(
                {
                    "question": str(q["question"]).strip(),
                    "correct_answer": str(q["correct_answer"]).strip(),
                    "incorrect_answers": [
                        str(a).strip() for a in q["incorrect_answers"]
                    ],
                }
            )
    return valid


# ─── GÉNÉRATION ───────────────────────────────────────────────────────────────
def generate_questions(clients, theme: str, difficulty: str) -> list:
    prompt = PROMPT_TEMPLATE.format(
        n=QUESTIONS_PER_COMBO,
        theme_desc=THEMES[theme],
        difficulty_desc=DIFFICULTIES[difficulty],
    )

    client_index = 0
    for attempt in range(1, MAX_RETRIES * len(clients) + 1):
        client = clients[client_index]
        try:
            print(f"    Tentative {attempt} (clé {client_index + 1}/{len(clients)})...", end=" ", flush=True)
            response = client.models.generate_content(
                model="gemini-2.5-flash",
                contents=prompt,
            )
            questions = extract_json(response.text)
            questions = validate_questions(questions)

            if len(questions) < 10:
                raise ValueError(
                    f"Seulement {len(questions)} questions valides (minimum 10)"
                )

            print(f"✓ {len(questions)} questions")
            return questions

        except Exception as e:
            err_str = str(e)
            # Quota épuisé sur cette clé → basculer sur la suivante
            if "429" in err_str and "GenerateRequestsPerDayPerProjectPerModel" in err_str:
                print(f"✗ quota épuisé (clé {client_index + 1})")
                client_index = (client_index + 1) % len(clients)
                if client_index == 0:
                    # Toutes les clés épuisées
                    break
                print(f"    Bascule sur la clé {client_index + 1}...")
                time.sleep(2)
            else:
                print(f"✗ {e}")
                if attempt < MAX_RETRIES * len(clients):
                    wait = DELAY_BETWEEN_CALLS * min(attempt, 3)
                    print(f"    Attente {wait}s avant retry...")
                    time.sleep(wait)

    raise RuntimeError(
        f"Échec après {MAX_RETRIES * len(clients)} tentatives pour {theme}/{difficulty}"
    )


# ─── MAIN ─────────────────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="Génère les questions Agora via Gemini")
    parser.add_argument("--force", action="store_true", help="Régénère tous les fichiers même existants")
    parser.add_argument("--theme", type=str, help="Génère uniquement ce thème (ex: HISTOIRE)")
    parser.add_argument("--difficulty", type=str, help="Génère uniquement cette difficulté (ex: DEBUTANT)")
    args = parser.parse_args()

    # Préparer le dossier de sortie
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # Configurer Gemini — créer un client par clé
    gemini_keys = load_gemini_keys()
    clients = [genai.Client(api_key=key) for key in gemini_keys]
    print(f"  {len(clients)} clé(s) API configurée(s)")

    # Sélectionner les thèmes/difficultés à générer
    themes = [args.theme] if args.theme else list(THEMES.keys())
    difficulties = [args.difficulty] if args.difficulty else list(DIFFICULTIES.keys())

    total = len(themes) * len(difficulties)
    done = 0
    skipped = 0
    errors = []

    print(f"\n{'='*60}")
    print(f"  Génération Agora — {total} combinaisons à traiter")
    print(f"  Modèle : gemini-2.5-flash | Questions/combo : {QUESTIONS_PER_COMBO}")
    print(f"  Sortie : {os.path.abspath(OUTPUT_DIR)}")
    print(f"{'='*60}\n")

    for theme in themes:
        if theme not in THEMES:
            print(f"⚠ Thème inconnu : {theme}")
            continue

        for difficulty in difficulties:
            if difficulty not in DIFFICULTIES:
                print(f"⚠ Difficulté inconnue : {difficulty}")
                continue

            output_file = os.path.join(OUTPUT_DIR, f"{theme}_{difficulty}.json")
            label = f"{theme} / {difficulty}"

            if os.path.exists(output_file) and not args.force:
                print(f"  ⏭  {label} — déjà généré, ignoré (--force pour régénérer)")
                skipped += 1
                done += 1
                continue

            print(f"  [{done+1}/{total}] {label}")

            try:
                questions = generate_questions(clients, theme, difficulty)

                with open(output_file, "w", encoding="utf-8") as f:
                    json.dump(questions, f, ensure_ascii=False, indent=2)

                done += 1
                print(f"           → Sauvegardé : {os.path.basename(output_file)}")

            except Exception as e:
                errors.append((label, str(e)))
                print(f"  ✗ ERREUR : {e}")

            # Délai anti-rate-limit (sauf dernier appel)
            if done < total:
                time.sleep(DELAY_BETWEEN_CALLS)

    print(f"\n{'='*60}")
    print(f"  Terminé : {done - len(errors)} succès, {skipped} ignorés, {len(errors)} erreurs")
    if errors:
        print("\n  Combinaisons en erreur :")
        for label, err in errors:
            print(f"    - {label} : {err}")
    print(f"{'='*60}\n")


if __name__ == "__main__":
    main()
