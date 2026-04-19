package com.kapoue.agora.domain.model

import com.kapoue.agora.R

enum class Theme(
    val displayName: String,
    val otdCategoryId: Int,
    val unsplashQuery: String,
    val placeholderRes: Int
) {
    HISTOIRE(
        displayName = "Histoire",
        otdCategoryId = 23,
        unsplashQuery = "ancient history monument",
        placeholderRes = R.drawable.placeholder_histoire
    ),
    GEOGRAPHIE(
        displayName = "Géographie",
        otdCategoryId = 22,
        unsplashQuery = "world map geography landscape",
        placeholderRes = R.drawable.placeholder_geographie
    ),
    SCIENCES(
        displayName = "Sciences & Nature",
        otdCategoryId = 17,
        unsplashQuery = "science laboratory nature",
        placeholderRes = R.drawable.placeholder_sciences
    ),
    CINEMA(
        displayName = "Cinéma",
        otdCategoryId = 11,
        unsplashQuery = "cinema film camera",
        placeholderRes = R.drawable.placeholder_cinema
    ),
    MUSIQUE(
        displayName = "Musique",
        otdCategoryId = 12,
        unsplashQuery = "music instruments concert",
        placeholderRes = R.drawable.placeholder_musique
    ),
    SPORT(
        displayName = "Sport",
        otdCategoryId = 21,
        unsplashQuery = "sport stadium athletic",
        placeholderRes = R.drawable.placeholder_sport
    ),
    LITTERATURE(
        displayName = "Littérature",
        otdCategoryId = 10,
        unsplashQuery = "books library literature",
        placeholderRes = R.drawable.placeholder_litterature
    ),
    INFORMATIQUE(
        displayName = "Informatique",
        otdCategoryId = 18,
        unsplashQuery = "computer technology code",
        placeholderRes = R.drawable.placeholder_informatique
    ),
    ART(
        displayName = "Art",
        otdCategoryId = 25,
        unsplashQuery = "art painting gallery museum",
        placeholderRes = R.drawable.placeholder_art
    ),
    MATHEMATIQUES(
        displayName = "Mathématiques",
        otdCategoryId = 19,
        unsplashQuery = "mathematics geometry abstract",
        placeholderRes = R.drawable.placeholder_mathematiques
    ),
    MYTHOLOGIE(
        displayName = "Mythologie",
        otdCategoryId = 20,
        unsplashQuery = "greek mythology ancient gods",
        placeholderRes = R.drawable.placeholder_mythologie
    ),
    ANIMAUX(
        displayName = "Animaux",
        otdCategoryId = 27,
        unsplashQuery = "wildlife animals nature",
        placeholderRes = R.drawable.placeholder_animaux
    ),
    VEHICULES(
        displayName = "Véhicules",
        otdCategoryId = 28,
        unsplashQuery = "vehicles cars transportation",
        placeholderRes = R.drawable.placeholder_vehicules
    )
}
