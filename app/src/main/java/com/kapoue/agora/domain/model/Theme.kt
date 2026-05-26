package com.kapoue.agora.domain.model

import com.kapoue.agora.R

enum class Theme(
    val displayName: String,
    val unsplashQuery: String,
    val placeholderRes: Int
) {
    HISTOIRE(
        displayName = "Histoire",
        unsplashQuery = "ancient history monument",
        placeholderRes = R.drawable.placeholder_histoire
    ),
    GEOGRAPHIE(
        displayName = "Géographie",
        unsplashQuery = "world map geography landscape",
        placeholderRes = R.drawable.placeholder_geographie
    ),
    SCIENCES(
        displayName = "Sciences & Nature",
        unsplashQuery = "science laboratory nature",
        placeholderRes = R.drawable.placeholder_sciences
    ),
    CINEMA(
        displayName = "Cinéma",
        unsplashQuery = "cinema film camera",
        placeholderRes = R.drawable.placeholder_cinema
    ),
    MUSIQUE(
        displayName = "Musique",
        unsplashQuery = "music instruments concert",
        placeholderRes = R.drawable.placeholder_musique
    ),
    SPORT(
        displayName = "Sport",
        unsplashQuery = "sport stadium athletic",
        placeholderRes = R.drawable.placeholder_sport
    ),
    LITTERATURE(
        displayName = "Littérature",
        unsplashQuery = "books library literature",
        placeholderRes = R.drawable.placeholder_litterature
    ),
    INFORMATIQUE(
        displayName = "Informatique",
        unsplashQuery = "computer technology code",
        placeholderRes = R.drawable.placeholder_informatique
    ),
    ART(
        displayName = "Art",
        unsplashQuery = "art painting gallery museum",
        placeholderRes = R.drawable.placeholder_art
    ),
    MATHEMATIQUES(
        displayName = "Mathématiques",
        unsplashQuery = "mathematics geometry abstract",
        placeholderRes = R.drawable.placeholder_mathematiques
    ),
    MYTHOLOGIE(
        displayName = "Mythologie",
        unsplashQuery = "greek mythology ancient gods",
        placeholderRes = R.drawable.placeholder_mythologie
    ),
    ANIMAUX(
        displayName = "Animaux",
        unsplashQuery = "wildlife animals nature",
        placeholderRes = R.drawable.placeholder_animaux
    ),
    VEHICULES(
        displayName = "Véhicules",
        unsplashQuery = "vehicles cars transportation",
        placeholderRes = R.drawable.placeholder_vehicules
    ),
    GASTRONOMIE(
        displayName = "Gastronomie",
        unsplashQuery = "gourmet food cuisine chef",
        placeholderRes = R.drawable.placeholder_gastronomie
    ),
    JEUX_VIDEO(
        displayName = "Jeux vidéo",
        unsplashQuery = "video games controller gaming",
        placeholderRes = R.drawable.placeholder_jeux_video
    ),
    CULTURE_GENERALE(
        displayName = "Culture Générale",
        unsplashQuery = "knowledge culture library",
        placeholderRes = R.drawable.placeholder_culture_generale
    )
}
