package com.coderkillers.guiafacil.data.model

data class CheckableStep(
    val id: Int,
    val text: String,
    val isCompleted: Boolean = false
)

data class SavedProcedure(
    val id: String,
    val title: String,
    val summary: String,
    val steps: List<CheckableStep>,
    val savedAt: Long = System.currentTimeMillis(),
    val officialInfo: String = ""
)

data class AssistantResponse(
    val question: String,
    val summaryText: String,
    val steps: List<String>,
    val officialInfo: String,
    val nombreModulo: String = "",
    val horarioAtencion: String = "",
    val busquedaMaps: String = "",
    val enlaceOficial: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val isSaved: Boolean = false
)

data class DocumentAnalysisResult(
    val summary: String,
    val steps: List<CheckableStep>,
    val isSaved: Boolean = false
)

data class GuiaFacilUiState(
    val selectedLanguage: String = "Español",
    val isTranslatingUi: Boolean = false,
    val dictionary: Map<String, String> = defaultSpanishDictionary,
    val currentProfile: String = "Adulto mayor",
    val readingLevel: String = "Muy sencillo",
    val assistantResponse: AssistantResponse? = null,
    val isAssistantLoading: Boolean = false,
    val documentResult: DocumentAnalysisResult? = null,
    val isDocumentLoading: Boolean = false,
    val savedProcedures: List<SavedProcedure> = emptyList(),
    val errorMessage: String? = null,
    val isListeningVoice: Boolean = false,
    val spokenText: String = "",
    val showHelpDialog: Boolean = false
)

val defaultSpanishDictionary: Map<String, String> = mapOf(
    "app_title" to "GuíaFácil",
    "app_subtitle" to "Puentes de Oportunidad",
    "welcome_title" to "Bienvenido a GuíaFácil — Elige tu idioma o lengua para comenzar",
    "custom_lang_placeholder" to "¿No encuentras tu idioma? Escríbelo aquí",
    "btn_continue" to "Continuar",
    "home_header" to "Hola, ¿en qué te ayudo hoy?",
    "card_assistant_title" to "Hablar con GuíaFácil",
    "card_assistant_desc" to "Pregunta por voz o texto sobre pensiones, becas o trámites",
    "card_reader_title" to "Leer un Documento",
    "card_reader_desc" to "Toma foto a recetas médicas, cartas de becas o citatorios",
    "card_saved_title" to "Mis Trámites Guardados",
    "card_saved_desc" to "Consulta guías y listas de verificación sin internet",
    "footer_text" to "A tu ritmo. Siempre contigo.",
    "btn_need_help" to "¿Necesitas ayuda?",
    "assistant_title" to "Hablemos con calma",
    "profile_label" to "Perfil del usuario:",
    "profile_elderly" to "Adulto mayor",
    "profile_student" to "Estudiante 1ra generación",
    "profile_migrant" to "Migrante",
    "profile_low_conn" to "Baja conectividad",
    "reading_level_label" to "Nivel de lectura:",
    "level_simple" to "Muy sencillo",
    "level_standard" to "Estándar",
    "suggested_questions_title" to "Preguntas rápidas sugeridas:",
    "q_pension" to "Pensión Bienestar para Adultos Mayores",
    "q_scholarship" to "Becas Universitarias de Primera Generación",
    "q_health" to "Atención médica gratuita sin seguro social",
    "q_migrant" to "Trámites de regulación y empleo para migrantes",
    "input_placeholder" to "Escribe o presiona el micrófono para hablar...",
    "btn_listen" to "Escuchar respuesta",
    "btn_save" to "Guardar lista",
    "btn_official_link" to "Ver módulo o enlace oficial",
    "reader_title" to "Leer un documento",
    "btn_take_photo" to "Tomar Foto y Explicar",
    "btn_choose_gallery" to "Elegir foto de la galería",
    "btn_demo_prescription" to "Probar con receta de ejemplo",
    "summary_title" to "Resumen sencillo:",
    "steps_title" to "Mis siguientes pasos:",
    "saved_title" to "Mis Trámites Guardados (Modo Offline)",
    "saved_empty" to "No tienes trámites guardados aún.",
    "btn_delete" to "Eliminar",
    "offline_badge" to "Disponible sin internet",
    "help_dialog_title" to "Guía Rápida de Uso y Apoyo",
    "help_dialog_content" to "GuíaFácil te acompaña en tus trámites:\n\n1. Habla o escribe tus dudas en tu idioma preferido.\n2. Fotografía tus documentos para entenderlos sin enredos.\n3. Guarda tus pasos para consultarlos aun si no tienes internet.\n\n📞 Línea de atención directa: 800-00-GUIA (800-00-4842)\nAtención gratuita y empática.",
    "btn_close" to "Cerrar",
    "loading_translating_ui" to "Adaptando toda la interfaz con inteligencia empática...",
    "reader_desc" to "Toma o sube una foto de tu receta médica, citatorio o carta oficial para recibir una explicación clara sin palabras complicadas.",
    "doc_explanation_header" to "Explicación de tu documento",
    "btn_listen_voice" to "Escuchar en voz alta",
    "btn_save_offline" to "Guardar para ver sin internet",
    "completed_counter" to "completados",
    "saved_offline_success" to "Guardado para consultar sin internet",
    "loading_document_vision" to "Leyendo documento con visión inteligente..."
)
