package com.coderkillers.guiafacil.data.remote

import android.graphics.Bitmap
import android.util.Log
import com.coderkillers.guiafacil.BuildConfig
import com.coderkillers.guiafacil.data.model.AssistantResponse
import com.coderkillers.guiafacil.data.model.CheckableStep
import com.coderkillers.guiafacil.data.model.DocumentAnalysisResult
import com.coderkillers.guiafacil.data.model.defaultSpanishDictionary
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IpLocationResult(
    val city: String,
    val region: String,
    val country: String,
    val lat: Double?,
    val lon: Double?
)

class GeminiRepository {

    private val apiKey: String = BuildConfig.GEMINI_API_KEY
    private val gson = Gson()

    private val FREE_FLASH_MODELS = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-3.6-flash",
        "gemini-3.8-flash",
        "gemini-3.5-flash"
    )

    private fun isApiKeyValid(): Boolean {
        return apiKey.isNotBlank() && apiKey != "tu_clave" && apiKey != "YOUR_GEMINI_API_KEY"
    }

    // Configurado con responseMimeType = "application/json", timeout configurable (default 180,000 ms), temperatura y maxOutputTokens opcionales
    private fun createGenerativeModel(
        preferredName: String = "gemini-3.5-flash-lite",
        temperature: Float? = null,
        maxOutputTokens: Int? = null,
        timeoutMs: Long = 180_000L
    ): GenerativeModel {
        return GenerativeModel(
            modelName = preferredName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature?.let { this.temperature = it }
                maxOutputTokens?.let { this.maxOutputTokens = it }
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
            ),
            requestOptions = RequestOptions(timeout = timeoutMs)
        )
    }

    private fun normalizeBitmap(bitmap: Bitmap): Bitmap {
        val maxTargetDimension = 800
        val width = bitmap.width
        val height = bitmap.height
        val longestSide = maxOf(width, height)

        val resized = if (longestSide > maxTargetDimension) {
            val scale = maxTargetDimension.toFloat() / longestSide
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        return try {
            val stream = java.io.ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()
            android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size) ?: resized
        } catch (e: Exception) {
            resized
        }
    }

    /**
     * Translates the entire UI dictionary into the target language using Gemini AI in 40-key chunks.
     */
    suspend fun translateUiDictionary(targetLanguage: String): Map<String, String> = withContext(Dispatchers.IO) {
        val cleanLang = targetLanguage.trim()
        Log.d("GuiaFacilGemini", "translateUiDictionary called for language: '$cleanLang', apiKeyValid: ${isApiKeyValid()}")

        if (cleanLang.equals("Español", ignoreCase = true)) {
            return@withContext defaultSpanishDictionary
        }

        if (isApiKeyValid()) {
            for (modelName in FREE_FLASH_MODELS) {
                try {
                    Log.d("GuiaFacilGemini", "Attempting Gemini translation with model: $modelName for language: $cleanLang")
                    val generativeModel = createGenerativeModel(modelName, timeoutMs = 30_000L)

                    val combinedMap = mutableMapOf<String, String>()
                    val chunks = defaultSpanishDictionary.entries.chunked(40)
                    var allChunksSucceeded = true

                    for ((index, chunk) in chunks.withIndex()) {
                        val chunkMap = chunk.associate { it.key to it.value }
                        val prompt = """
                            Traduce TODOS los textos de la interfaz de la aplicación ciudadana "GuíaFácil" del español al idioma o lengua "$cleanLang".
                            Traduce el 100% del texto al idioma $cleanLang de forma natural y comprensible para un hablante nativo. No dejes frases, palabras ni conectores en español; si un término moderno no tiene traducción directa en $cleanLang, explícalo usando palabras sencillas propias de esa lengua.
                            Mantén intacto el nombre propio "GuíaFácil", "CURP", "INE" e "IA".
                            Devuelve únicamente un objeto JSON puro con las traducciones. No agregues corchetes, paréntesis ni texto adicional.
                            Aquí está el fragmento $index del diccionario base en español:
                            ${gson.toJson(chunkMap)}
                        """.trimIndent()

                        val response = generativeModel.generateContent(prompt)
                        val jsonText = cleanJsonResponse(response.text ?: "")
                        Log.d("GuiaFacilGemini", "Chunk $index response received from $modelName. Length: ${jsonText.length}")

                        if (jsonText.isNotBlank() && jsonText.startsWith("{")) {
                            val translatedChunk: Map<String, String> = gson.fromJson(
                                jsonText,
                                object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                            )
                            if (translatedChunk.isNotEmpty()) {
                                combinedMap.putAll(translatedChunk)
                            } else {
                                allChunksSucceeded = false
                                break
                            }
                        } else {
                            allChunksSucceeded = false
                            break
                        }
                    }

                    if (allChunksSucceeded && combinedMap.isNotEmpty()) {
                        Log.d("GuiaFacilGemini", "Successfully translated dictionary via model $modelName for $cleanLang (${combinedMap.size} keys)")
                        return@withContext combinedMap
                    }
                } catch (e: Throwable) {
                    Log.e("GuiaFacilGemini", "Error with model $modelName for $cleanLang", e)
                }
            }
        }

        Log.w("GuiaFacilGemini", "Gemini API translation skipped or failed for '$cleanLang'. Using fallback dictionary.")
        getFallbackDictionary(cleanLang)
    }

    private suspend fun fetchIpLocation(): IpLocationResult? = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("https://ipwho.is/")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = gson.fromJson(jsonText, JsonObject::class.java)
                if (jsonObj.get("success")?.asBoolean == true) {
                    val city = jsonObj.get("city")?.asString ?: ""
                    val region = jsonObj.get("region")?.asString ?: ""
                    val country = jsonObj.get("country")?.asString ?: ""
                    val lat = jsonObj.get("latitude")?.asDouble
                    val lon = jsonObj.get("longitude")?.asDouble
                    return@withContext IpLocationResult(city, region, country, lat, lon)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val url = java.net.URL("http://ip-api.com/json/?fields=status,city,regionName,country,lat,lon")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = gson.fromJson(jsonText, JsonObject::class.java)
                if (jsonObj.get("status")?.asString == "success") {
                    val city = jsonObj.get("city")?.asString ?: ""
                    val region = jsonObj.get("regionName")?.asString ?: ""
                    val country = jsonObj.get("country")?.asString ?: ""
                    val lat = jsonObj.get("lat")?.asDouble
                    val lon = jsonObj.get("lon")?.asDouble
                    return@withContext IpLocationResult(city, region, country, lat, lon)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
    }

    /**
     * Transcribe audio and generates a step-by-step clear response for citizen procedures using Gemini Chat.
     */
    suspend fun generateAssistantResponseFromAudio(
        audioBytes: ByteArray,
        profile: String,
        readingLevel: String,
        targetLanguage: String,
        locationContext: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ): AssistantResponse = withContext(Dispatchers.IO) {
        val cleanLang = targetLanguage.trim()

        // Nivel 2 — Respaldo por IP si no se otorgó GPS o fue null
        var finalLocationContext = locationContext
        var finalLat = lat
        var finalLng = lng

        if (finalLocationContext.isNullOrBlank()) {
            val ipLoc = fetchIpLocation()
            if (ipLoc != null) {
                finalLocationContext = """
                    Ubicación estimada por IP: ${ipLoc.city}, ${ipLoc.region} (coordenadas: ${ipLoc.lat}, ${ipLoc.lon}). IMPORTANTE: Usa esta ubicación aproximada ÚNICAMENTE si coincide geográficamente con la consulta o lengua del usuario (por ejemplo, si pregunta por un trámite local de ese estado o habla una lengua indígena de esa región). Si la consulta menciona otro estado/ciudad distinto o la IP parece estar en otro estado por el proveedor de telefonía móvil, ignora la ubicación por IP y da una respuesta general nacional sin inventar un módulo local erróneo.
                """.trimIndent()
                if (finalLat == null) finalLat = ipLoc.lat
                if (finalLng == null) finalLng = ipLoc.lon
            }
        }

        val locInstruction = if (!finalLocationContext.isNullOrBlank()) {
            """
            INFORMACIÓN DE GEOLOCALIZACIÓN DEL USUARIO:
            $finalLocationContext
            
            INSTRUCCIONES DE UBICACIÓN Y MÓDULO:
            - Si la ubicación es válida (GPS o IP coincidente con la consulta), indica el nombre del módulo u oficina oficial más cercana en 'nombreModulo', su horario de atención traducido a $cleanLang en 'horarioAtencion', y una búsqueda optimizada para Google Maps en 'busquedaMaps' (ejemplo: "Módulo del Bienestar cerca de Villahermosa Tabasco").
            - Si la ubicación no coincide o es ambigua, deja 'busquedaMaps' como cadena vacía "" y proporciona información oficial nacional.
            """.trimIndent()
        } else {
            "No se dispone de ubicación GPS ni IP. Proporciona información general oficial y coloca 'busquedaMaps': ''."
        }

        if (isApiKeyValid()) {
            for (modelName in FREE_FLASH_MODELS) {
                try {
                    Log.d("GuiaFacilGemini", "Generating assistant response from audio with model: $modelName")
                    val generativeModel = createGenerativeModel(modelName, timeoutMs = 45_000L)
                    val prompt = """
                        Actúa como GuíaFácil, un asistente empático y accesible de trámites sociales y salud en México.
                        
                        Paso 1: Transcribe fielmente lo que el usuario dice en el archivo de audio adjunto. La transcripción debe estar en el campo 'transcripcion'. No cambies el sentido de la pregunta.
                        
                        Paso 2: Responde a la duda transcrita.
                        Perfil del usuario: $profile. Nivel de lectura: $readingLevel.
                        Idioma obligatorio de la respuesta: "$cleanLang".
                        
                        $locInstruction
                        
                        Traduce el 100% de la respuesta y de todos los campos al idioma $cleanLang de forma natural y comprensible para un hablante nativo. No dejes palabras ni frases mezcladas en español.
                        
                        Responde ESTRICTAMENTE con un objeto JSON válido con los siguientes campos en $cleanLang:
                        {
                          "transcripcion": "La transcripción exacta de lo que dice el audio.",
                          "summaryText": "Resumen claro en 2 o 3 oraciones sencillas en $cleanLang sin tecnicismos.",
                          "steps": ["1. Paso inicial...", "2. Siguiente paso...", "3. Paso final..."],
                          "officialInfo": "Información oficial de contacto (ejemplo: Módulo de Atención Bienestar más cercano o teléfono 800-639-4264).",
                          "nombreModulo": "Nombre de la oficina, clínica o módulo oficial más cercano.",
                          "horarioAtencion": "Horario habitual de atención traducido a $cleanLang (ejemplo: 'Lunes a viernes de 8:00 a 15:00 hrs').",
                          "busquedaMaps": "Cadena optimizada para buscar en Google Maps (ejemplo: 'Módulo del Bienestar cerca de Villahermosa Tabasco') o vacía '' si no aplica.",
                          "enlaceOficial": "URL oficial de la dependencia (ejemplo: 'https://www.gob.mx/bienestar')."
                        }
                    """.trimIndent()

                    val inputContent = content {
                        blob("audio/mp4", audioBytes)
                        text(prompt)
                    }

                    val response = generativeModel.generateContent(inputContent)
                    val jsonText = cleanJsonResponse(response.text ?: "")
                    if (jsonText.isNotBlank() && jsonText.startsWith("{")) {
                        val jsonObj = gson.fromJson(jsonText, JsonObject::class.java)

                        val transcription = jsonObj.get("transcripcion")?.asString ?: "Audio transcrito"
                        val summary = jsonObj.get("summaryText")?.asString ?: "Guía rápida para tu trámite."
                        val stepsArray = jsonObj.getAsJsonArray("steps")
                        val stepsList = mutableListOf<String>()
                        if (stepsArray != null) {
                            for (elem in stepsArray) {
                                stepsList.add(elem.asString)
                            }
                        }
                        if (stepsList.isEmpty()) {
                            stepsList.add("1. Acude al módulo oficial con tu identificación.")
                            stepsList.add("2. Solicita el formato de registro gratuito.")
                            stepsList.add("3. Conserva tu comprobante para seguimiento.")
                        }
                        val officialInfo = jsonObj.get("officialInfo")?.asString ?: "Teléfono de atención gratuita: 800-639-4264 | Ventanilla Única"
                        val nombreModulo = jsonObj.get("nombreModulo")?.asString ?: ""
                        val horarioAtencion = jsonObj.get("horarioAtencion")?.asString ?: ""
                        val busquedaMaps = jsonObj.get("busquedaMaps")?.asString ?: ""
                        val enlaceOficial = jsonObj.get("enlaceOficial")?.asString ?: "https://www.gob.mx"

                        return@withContext AssistantResponse(
                            question = transcription,
                            summaryText = summary,
                            steps = stepsList,
                            officialInfo = officialInfo,
                            nombreModulo = nombreModulo,
                            horarioAtencion = horarioAtencion,
                            busquedaMaps = busquedaMaps,
                            enlaceOficial = enlaceOficial,
                            lat = finalLat,
                            lng = finalLng
                        )
                    }
                } catch (e: Throwable) {
                    Log.e("GuiaFacilGemini", "Assistant audio error with model $modelName", e)
                }
            }
        }

        getFallbackAssistantResponse("Consulta por voz")
    }

    /**
     * Generates a step-by-step clear response for citizen procedures using Gemini Chat.
     */
    suspend fun generateAssistantResponse(
        question: String,
        profile: String,
        readingLevel: String,
        targetLanguage: String,
        locationContext: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ): AssistantResponse = withContext(Dispatchers.IO) {
        val cleanLang = targetLanguage.trim()

        // Nivel 2 — Respaldo por IP si no se otorgó GPS o fue null
        var finalLocationContext = locationContext
        var finalLat = lat
        var finalLng = lng

        if (finalLocationContext.isNullOrBlank()) {
            val ipLoc = fetchIpLocation()
            if (ipLoc != null) {
                finalLocationContext = """
                    Ubicación estimada por IP: ${ipLoc.city}, ${ipLoc.region} (coordenadas: ${ipLoc.lat}, ${ipLoc.lon}). IMPORTANTE: Usa esta ubicación aproximada ÚNICAMENTE si coincide geográficamente con la consulta o lengua del usuario (por ejemplo, si pregunta por un trámite local de ese estado o habla una lengua indígena de esa región). Si la consulta menciona otro estado/ciudad distinto o la IP parece estar en otro estado por el proveedor de telefonía móvil, ignora la ubicación por IP y da una respuesta general nacional sin inventar un módulo local erróneo.
                """.trimIndent()
                if (finalLat == null) finalLat = ipLoc.lat
                if (finalLng == null) finalLng = ipLoc.lon
            }
        }

        val locInstruction = if (!finalLocationContext.isNullOrBlank()) {
            """
            INFORMACIÓN DE GEOLOCALIZACIÓN DEL USUARIO:
            $finalLocationContext
            
            INSTRUCCIONES DE UBICACIÓN Y MÓDULO:
            - Si la ubicación es válida (GPS o IP coincidente con la consulta), indica el nombre del módulo u oficina oficial más cercana en 'nombreModulo', su horario de atención traducido a $cleanLang en 'horarioAtencion', y una búsqueda optimizada para Google Maps en 'busquedaMaps' (ejemplo: "Módulo del Bienestar cerca de Villahermosa Tabasco").
            - Si la ubicación no coincide o es ambigua, deja 'busquedaMaps' como cadena vacía "" y proporciona información oficial nacional.
            """.trimIndent()
        } else {
            "No se dispone de ubicación GPS ni IP. Proporciona información general oficial y coloca 'busquedaMaps': ''."
        }

        if (isApiKeyValid()) {
            for (modelName in FREE_FLASH_MODELS) {
                try {
                    Log.d("GuiaFacilGemini", "Generating assistant response with model: $modelName")
                    val generativeModel = createGenerativeModel(modelName, timeoutMs = 30_000L)
                    val prompt = """
                        Actúa como GuíaFácil, un asistente empático y accesible de trámites sociales y salud en México.
                        Responde a la duda: "$question".
                        Perfil del usuario: $profile. Nivel de lectura: $readingLevel.
                        Idioma obligatorio de la respuesta: "$cleanLang".
                        
                        $locInstruction
                        
                        Traduce el 100% de la respuesta y de todos los campos al idioma $cleanLang de forma natural y comprensible para un hablante nativo. No dejes palabras ni frases mezcladas en español.
                        
                        Responde ESTRICTAMENTE con un objeto JSON válido con los siguientes campos en $cleanLang:
                        {
                          "summaryText": "Resumen claro en 2 o 3 oraciones sencillas en $cleanLang sin tecnicismos.",
                          "steps": ["1. Paso inicial...", "2. Siguiente paso...", "3. Paso final..."],
                          "officialInfo": "Información oficial de contacto (ejemplo: Módulo de Atención Bienestar más cercano o teléfono 800-639-4264).",
                          "nombreModulo": "Nombre de la oficina, clínica o módulo oficial más cercano.",
                          "horarioAtencion": "Horario habitual de atención traducido a $cleanLang (ejemplo: 'Lunes a viernes de 8:00 a 15:00 hrs').",
                          "busquedaMaps": "Cadena optimizada para buscar en Google Maps (ejemplo: 'Módulo del Bienestar cerca de Villahermosa Tabasco') o vacía '' si no aplica.",
                          "enlaceOficial": "URL oficial de la dependencia (ejemplo: 'https://www.gob.mx/bienestar')."
                        }
                    """.trimIndent()

                    val response = generativeModel.generateContent(prompt)
                    val jsonText = cleanJsonResponse(response.text ?: "")
                    if (jsonText.isNotBlank() && jsonText.startsWith("{")) {
                        val jsonObj = gson.fromJson(jsonText, JsonObject::class.java)

                        val summary = jsonObj.get("summaryText")?.asString ?: "Guía rápida para tu trámite."
                        val stepsArray = jsonObj.getAsJsonArray("steps")
                        val stepsList = mutableListOf<String>()
                        if (stepsArray != null) {
                            for (elem in stepsArray) {
                                stepsList.add(elem.asString)
                            }
                        }
                        if (stepsList.isEmpty()) {
                            stepsList.add("1. Acude al módulo oficial con tu identificación.")
                            stepsList.add("2. Solicita el formato de registro gratuito.")
                            stepsList.add("3. Conserva tu comprobante para seguimiento.")
                        }
                        val officialInfo = jsonObj.get("officialInfo")?.asString ?: "Teléfono de atención gratuita: 800-639-4264 | Ventanilla Única"
                        val nombreModulo = jsonObj.get("nombreModulo")?.asString ?: ""
                        val horarioAtencion = jsonObj.get("horarioAtencion")?.asString ?: ""
                        val busquedaMaps = jsonObj.get("busquedaMaps")?.asString ?: ""
                        val enlaceOficial = jsonObj.get("enlaceOficial")?.asString ?: "https://www.gob.mx"

                        return@withContext AssistantResponse(
                            question = question,
                            summaryText = summary,
                            steps = stepsList,
                            officialInfo = officialInfo,
                            nombreModulo = nombreModulo,
                            horarioAtencion = horarioAtencion,
                            busquedaMaps = busquedaMaps,
                            enlaceOficial = enlaceOficial,
                            lat = finalLat,
                            lng = finalLng
                        )
                    }
                } catch (e: Throwable) {
                    Log.e("GuiaFacilGemini", "Assistant error with model $modelName", e)
                }
            }
        }

        getFallbackAssistantResponse(question)
    }

    /**
     * Analyzes document images using Gemini Vision with a strict 2-step flow:
     * Step 1: Real extraction in Spanish with temperature = 0.1f and exact visual details.
     * Step 2: Faithful translation to targetLanguage if cleanLang != "Español".
     */
    suspend fun analyzeDocumentImage(
        bitmap: Bitmap,
        targetLanguage: String
    ): DocumentAnalysisResult = withContext(Dispatchers.IO) {
        val cleanLang = targetLanguage.trim()
        val normalizedBitmap = normalizeBitmap(bitmap)

        if (isApiKeyValid()) {
            for (modelName in FREE_FLASH_MODELS) {
                try {
                    Log.d("GuiaFacilGemini", "Analyzing document with model: $modelName")
                    val visionModel = createGenerativeModel(
                        preferredName = modelName,
                        temperature = 0.1f,
                        maxOutputTokens = 1024,
                        timeoutMs = 25_000L
                    )
                    val visionPrompt = """
                        Analiza esta imagen con precisión.
                        1. Explica de forma directa y sencilla qué es este documento y cuál es su mensaje principal. Si es una receta o documento médico, incluye qué tiene el paciente (diagnóstico o síntomas), el médico/clínica, el paciente y la fecha. Si NO es un documento médico (por ejemplo: un cartel, invitación, recibo, citatorio, acta o aviso), NO digas 'Este documento no es una receta médica'; simplemente describe directamente de qué trata el documento, quién lo emite, fechas, lugares y datos clave. Coloca toda esta información en el campo 'explicacion'.
                        
                        2. En 'pasos', extrae ÚNICAMENTE acciones concretas, reales y específicas que la persona deba hacer según lo que está escrito en la imagen:
                        - Si es receta médica: solo medicamentos (dosis, horario, días), estudios o reposo.
                        - Si es invitación, cartel o evento: solo acciones prácticas del evento (ej. 'Asistir al evento el 4 de diciembre de 2026 antes de las 5:00 p.m.', 'Acudir acompañado de la familia').
                        - Si es recibo, citatorio o trámite oficial: solo las fechas límite, montos a pagar, lugares a acudir o requisitos reales escritos en la hoja.
                        - Prohibido inventar pasos genéricos de relleno como 'Verificar los datos' o 'Presentar en el módulo' si la imagen no menciona ningún módulo ni trámite. Si el documento es puramente informativo y solo tiene 1 o 2 acciones reales, devuelve solo esas (pueden ser 0, 1, 2 o 3 pasos, no obligues a rellenar 3). Si no hay acciones requeridas, deja el array 'pasos' vacío.
                        
                        Devuelve únicamente un JSON con este formato: {"explicacion": "...", "pasos": ["Acción 1", "Acción 2"]}.
                    """.trimIndent()

                    val visionContent = content {
                        image(normalizedBitmap)
                        text(visionPrompt)
                    }

                    val visionResponse = visionModel.generateContent(visionContent)
                    val visionJsonText = cleanJsonResponse(visionResponse.text ?: "")

                    if (visionJsonText.isNotBlank() && visionJsonText.startsWith("{")) {
                        val jsonObj = gson.fromJson(visionJsonText, JsonObject::class.java)
                        val rawExplicacion = jsonObj.get("explicacion")?.asString
                            ?: jsonObj.get("summaryText")?.asString
                            ?: "Documento analizado con éxito."

                        val rawPasosArray = jsonObj.getAsJsonArray("pasos") ?: jsonObj.getAsJsonArray("steps")
                        val rawPasosList = mutableListOf<String>()
                        if (rawPasosArray != null && rawPasosArray.size() > 0) {
                            for (elem in rawPasosArray) {
                                rawPasosList.add(elem.asString)
                            }
                        }

                        // Si el idioma es Español, devolvemos directamente la extracción del Paso 1
                        if (cleanLang.equals("Español", ignoreCase = true)) {
                            val checkableSteps = rawPasosList.mapIndexed { idx, stepText ->
                                CheckableStep(id = idx + 1, text = stepText, isCompleted = false)
                            }
                            return@withContext DocumentAnalysisResult(
                                summary = rawExplicacion,
                                steps = checkableSteps
                            )
                        }

                        // PASO 2: Traducción fiel al idioma seleccionado
                        for (tModelName in FREE_FLASH_MODELS) {
                            try {
                                Log.d("GuiaFacilGemini", "Translating document result with model: $tModelName")
                                val translationModel = createGenerativeModel(
                                    preferredName = tModelName,
                                    temperature = 0.1f,
                                    maxOutputTokens = 1024,
                                    timeoutMs = 25_000L
                                )
                                val inputSpanishJson = gson.toJson(
                                    mapOf(
                                        "explicacion" to rawExplicacion,
                                        "pasos" to rawPasosList
                                    )
                                )
                                val translationPrompt = """
                                    Traduce el 100% del texto al idioma $cleanLang de forma natural y comprensible para un hablante nativo. No dejes frases, palabras ni conectores en español; si un término médico moderno (como 'cápsula' o 'diagnóstico') no tiene traducción directa en $cleanLang, explícalo usando palabras sencillas propias de esa lengua. Mantén exactamente los mismos hechos, números, horarios, nombres propios y medicamentos del texto original en español, sin omitir ni inventar datos.
                                    
                                    Input JSON:
                                    $inputSpanishJson
                                """.trimIndent()

                                val translationResponse = translationModel.generateContent(translationPrompt)
                                val translationJsonText = cleanJsonResponse(translationResponse.text ?: "")

                                if (translationJsonText.isNotBlank() && translationJsonText.startsWith("{")) {
                                    val translatedObj = gson.fromJson(translationJsonText, JsonObject::class.java)
                                    val translatedExplicacion = translatedObj.get("explicacion")?.asString ?: rawExplicacion
                                    val translatedPasosArray = translatedObj.getAsJsonArray("pasos")

                                    val translatedStepsList = mutableListOf<CheckableStep>()
                                    if (translatedPasosArray != null && translatedPasosArray.size() > 0) {
                                        var idx = 1
                                        for (elem in translatedPasosArray) {
                                            translatedStepsList.add(CheckableStep(id = idx, text = elem.asString, isCompleted = false))
                                            idx++
                                        }
                                    } else {
                                        rawPasosList.forEachIndexed { idx, s ->
                                            translatedStepsList.add(CheckableStep(id = idx + 1, text = s, isCompleted = false))
                                        }
                                    }

                                    return@withContext DocumentAnalysisResult(
                                        summary = translatedExplicacion,
                                        steps = translatedStepsList
                                    )
                                }
                            } catch (t: Throwable) {
                                Log.e("GuiaFacilGemini", "Step 2 translation failed for $cleanLang with model $tModelName", t)
                            }
                        }

                        // Fallback a extracción del Paso 1 si falló la traducción del Paso 2
                        val checkableSteps = rawPasosList.mapIndexed { idx, stepText ->
                            CheckableStep(id = idx + 1, text = stepText, isCompleted = false)
                        }
                        return@withContext DocumentAnalysisResult(
                            summary = rawExplicacion,
                            steps = checkableSteps
                        )
                    }
                } catch (e: Throwable) {
                    Log.e("GuiaFacilGemini", "Error analyzing document with model $modelName", e)
                }
            }
        }

        getFallbackDocumentResult()
    }

    private fun cleanJsonResponse(raw: String): String {
        val startIndex = raw.indexOf('{')
        val endIndex = raw.lastIndexOf('}')
        if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
            return raw.substring(startIndex, endIndex + 1)
        }
        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    // --- IDIOMAS EXTRAÍDOS DIRECTAMENTE DEL HTML COMO RESPALDO POR SI FALLA LA API ---

    private fun getFallbackDictionary(language: String): Map<String, String> {
        val lower = language.lowercase()

        val isChontal = lower.contains("chontal") || lower.contains("yokot'an") || lower.contains("chol") || lower.contains("ch'ol")
        val isTzotzil = lower.contains("tzotzil") || lower.contains("tsotsil")
        val isMaya = lower.contains("maya")
        val isNahuatl = lower.contains("náhuatl") || lower.contains("nahuatl")
        val isZoque = lower.contains("zoque")
        val isHaitian = lower.contains("kreyòl") || lower.contains("criollo") || lower.contains("haitiano") || lower.contains("ht")
        val isEnglish = lower.contains("english") || lower.contains("inglés") || lower.contains("en")

        if (isChontal) {
            return mapOf(
                "app_title" to "GuíaFácil",
                "app_subtitle" to "I Beilij Kolbal",
                "welcome_title" to "K'ajti' a wut tuba GuíaFácil",
                "custom_lang_placeholder" to "¿Mach a chän a t'an? Ts'ibän wä'e",
                "btn_continue" to "Bixik",
                "home_header" to "K'ajti' a wut, ¿kwa' k'ä k'elänet?",
                "card_assistant_title" to "T'an yik'ot GuíaFácil",
                "card_assistant_desc" to "K'ajti' yik'ot a t'an o ts'ibän",
                "card_reader_title" to "K'ele jun jun",
                "card_reader_desc" to "Ch'äben foto a ts'ak o jun",
                "card_saved_title" to "K'ä jun lot'ol",
                "card_saved_desc" to "K'ele a jun mach'an internet",
                "footer_text" to "Tä a k'in. Junjun k'in yik'otet.",
                "btn_need_help" to "¿A k'äne' k'ä'älibal?",
                "assistant_title" to "T'anon tä utz",
                "profile_label" to "A k'aba':",
                "profile_elderly" to "Nojoch winik",
                "profile_student" to "Ajkänon",
                "profile_migrant" to "Ajxänbal",
                "profile_low_conn" to "Mach'an internet",
                "reading_level_label" to "Nivel de lectura:",
                "level_simple" to "Toj säk",
                "level_standard" to "Utz",
                "suggested_questions_title" to "K'ajti'ya:",
                "q_pension" to "¿Kwa' k'ä k'äne' tuba pensión de nojoch winik?",
                "q_scholarship" to "¿Kwa' k'ä k'äne' tuba beca de ajkänon?",
                "q_health" to "¿Tuba an ts'ak mach'an tojol?",
                "q_migrant" to "¿Kwa' k'ä k'äne' tuba patan yik'ot CURP?",
                "input_placeholder" to "Ts'ibän wä'e a k'ajti'...",
                "btn_listen" to "Uybin t'an",
                "btn_save" to "Lot'o jun",
                "btn_official_link" to "Bixik tä oficina",
                "reader_title" to "K'ele jun jun",
                "btn_take_photo" to "Ch'äben Foto yik'ot T'an",
                "btn_choose_gallery" to "Yajkan foto",
                "btn_demo_prescription" to "O k'ele jun jun de ejemplo:",
                "summary_title" to "T'an toj säk",
                "steps_title" to "K'ä xänbal",
                "saved_title" to "K'ä jun lot'ol",
                "saved_empty" to "Mach'an jun lot'ol.",
                "btn_delete" to "Tupo",
                "offline_badge" to "Mach'an internet",
                "help_dialog_title" to "¿A k'äne' k'ä'älibal?",
                "help_dialog_content" to "GuíaFácil k'ä k'elänet tä a t'an.\n\n1. K'ajti' yik'ot a t'an.\n2. Ch'äben foto a ts'ak o jun.\n3. Lot'o tuba mach'an internet.",
                "btn_close" to "Mak-o",
                "btn_consult" to "Täskän",
                "voice_prompt" to "K'ajti' kwa' a k'äne'..."
            )
        }

        if (isTzotzil) {
            return mapOf(
                "app_title" to "GuíaFácil",
                "app_subtitle" to "Sbeiltasobel cha'an lekil k'usi",
                "welcome_title" to "Ch'ambil ta GuíaFácil",
                "custom_lang_placeholder" to "¿Mi mu xata ak'op? Ts'ibao li'i",
                "btn_continue" to "Batan",
                "home_header" to "K'uxi, ¿k'usi chajkolta o?",
                "card_assistant_title" to "K'opojan xchi'uk GuíaFácil",
                "card_assistant_desc" to "Jak'o ta ak'op o ts'ibao",
                "card_reader_title" to "K'elo jun vun",
                "card_reader_desc" to "Lok'tao ta foto poxil o vun",
                "card_saved_title" to "Kvuntak k'ejbil",
                "card_saved_desc" to "K'elo avuntak mi ch'abal internet",
                "footer_text" to "Ta k'un. Xchi'ukot scotol k'ak'al.",
                "btn_need_help" to "¿Mi chak'an koltael?",
                "assistant_title" to "K'opojkutik ta k'un",
                "profile_label" to "Avabtel:",
                "profile_elderly" to "Mol / Me'el",
                "profile_student" to "Jchanvun",
                "profile_migrant" to "Jbejel",
                "profile_low_conn" to "Ch'abal internet",
                "reading_level_label" to "Nivel de lectura:",
                "level_simple" to "K'un ta a'iel",
                "level_standard" to "Lek",
                "suggested_questions_title" to "Jak'obil:",
                "q_pension" to "¿K'usi sk'an sventa pensión yu'un mol me'el?",
                "q_scholarship" to "¿K'uxi ta k'anel beca sventa jchanvun?",
                "q_health" to "¿Bu oy poxil ti mu'yuk tojol?",
                "q_migrant" to "¿K'uxi ta pasel permiso yu'un abtel xchi'uk CURP?",
                "input_placeholder" to "Ts'ibao li'i k'usi chajak'...",
                "btn_listen" to "A'io stak'obil",
                "btn_save" to "K'ejo vun",
                "btn_official_link" to "Batan ta oficina",
                "reader_title" to "K'elo jun vun",
                "btn_take_photo" to "Lok'tao Foto xchi'uk Alo",
                "btn_choose_gallery" to "T'ujo foto",
                "btn_demo_prescription" to "O k'elo jun vun de ejemplo:",
                "summary_title" to "K'un ta a'iel",
                "steps_title" to "K'usi chajpas",
                "saved_title" to "Kvuntak k'ejbil",
                "saved_empty" to "Mu'yuk vunetik nak'bil.",
                "btn_delete" to "Tupo",
                "offline_badge" to "Ch'abal internet",
                "help_dialog_title" to "¿Mi chak'an koltael?",
                "help_dialog_content" to "GuíaFácil chajkolta:\n\n1. Jak'o ta ak'op.\n2. Lok'tao ta foto.\n3. K'ejo sventa ch'abal internet.",
                "btn_close" to "Mak-o",
                "btn_consult" to "Tak'o",
                "voice_prompt" to "K'opojan li'i..."
            )
        }

        if (isMaya) {
            return mapOf(
                "app_title" to "GuíaFácil",
                "app_subtitle" to "U Beel Oportunidad",
                "welcome_title" to "Ki'imak óolal ti' GuíaFácil",
                "custom_lang_placeholder" to "¿Ma' táan a kaxtik a t'aan? Ts'íibt waye'",
                "btn_continue" to "Táanil",
                "home_header" to "Ba'ax ka wa'alik, ¿bix je'el in wáantkech bejla'e'?",
                "card_assistant_title" to "T'aan yéetel GuíaFácil",
                "card_assistant_desc" to "K'áat yéetel a t'aan wa ts'íibt",
                "card_reader_title" to "Xok junp'éel ju'un",
                "card_reader_desc" to "Ch'a' foto ti' ts'aak wa ju'uno'ob",
                "card_saved_title" to "In Ju'uno'ob Ta'aka'an",
                "card_saved_desc" to "Il a ju'uno'ob kex mina'an internet",
                "footer_text" to "Ti' a wóol. Mantats' ta wéetel.",
                "btn_need_help" to "¿K'a'abéet wa tech áantaj?",
                "assistant_title" to "Ko'ox t'aan ich jets' óolal",
                "profile_label" to "A wóol:",
                "profile_elderly" to "Nuxib / X-nuk",
                "profile_student" to "J-xoknáal",
                "profile_migrant" to "J-xíimbal",
                "profile_low_conn" to "Mina'an internet",
                "reading_level_label" to "Nivel:",
                "level_simple" to "Jach chéen ch'a'abil",
                "level_standard" to "Ma'alob",
                "suggested_questions_title" to "K'áatchi'ob:",
                "q_pension" to "¿Ba'ax k'a'abéet utia'al pensión ti' nuxib?",
                "q_scholarship" to "¿Bix in k'áatik beca utia'al xoknáal?",
                "q_health" to "¿Tu'ux yaan ts'aak x-ma' bo'olil?",
                "q_migrant" to "¿Bix in beetik permiso ti' meyaj yéetel CURP?",
                "input_placeholder" to "Ts'íibt waye' ba'ax a k'áat...",
                "btn_listen" to "U'uy núukil",
                "btn_save" to "Ta'ak ju'un",
                "btn_official_link" to "Xeen ti' oficina",
                "reader_title" to "Xok junp'éel ju'un",
                "btn_take_photo" to "Ch'a' Foto yéetel Tsol",
                "btn_choose_gallery" to "Yéey foto",
                "btn_demo_prescription" to "Wa il junp'éel ju'un de ejemplo:",
                "summary_title" to "Tsolajil chéen ch'a'abil",
                "steps_title" to "Ba'ax ken in beet",
                "saved_title" to "In ju'uno'ob ta'aka'an",
                "saved_empty" to "Mina'an ju'uno'ob.",
                "btn_delete" to "Tu'ux",
                "offline_badge" to "Mina'an internet",
                "help_dialog_title" to "¿K'a'abéet wa tech áantaj?",
                "help_dialog_content" to "GuíaFácil:\n1. T'aan a t'aanil.\n2. Ch'a' foto.\n3. Ta'ak utia'al mina'an internet.",
                "btn_close" to "K'al",
                "btn_consult" to "Túuxt",
                "voice_prompt" to "T'aan..."
            )
        }

        if (isNahuatl) {
            return mapOf(
                "app_title" to "GuíaFácil",
                "app_subtitle" to "Ohtli tech Palehuia",
                "welcome_title" to "Ximopanolti ipan GuíaFácil",
                "custom_lang_placeholder" to "¿Amo tiknextia motlahtol? Xikihkuilo nikan",
                "btn_continue" to "Tlayekana",
                "home_header" to "Panolti, ¿tlen nimitspalehuis axkan?",
                "card_assistant_title" to "Xitlahto iwan GuíaFácil",
                "card_assistant_desc" to "Xitlajtlani ica motlahtol noso xikihkuilo",
                "card_reader_title" to "Xiktlachili se amatl",
                "card_reader_desc" to "Xikixkopina pajtli amatl noso amamej",
                "card_saved_title" to "Noamamej Ajoktokej",
                "card_saved_desc" to "Xikita moamamej kema amo onka internet",
                "footer_text" to "Ika moyolo. Nochipa mowaya.",
                "btn_need_help" to "¿Tikneki palehuilistli?",
                "assistant_title" to "Matitlahtokan ika pakilistli",
                "profile_label" to "Motekiyo:",
                "profile_elderly" to "Wewentsin / Ilamatsin",
                "profile_student" to "Momachtijketl",
                "profile_migrant" to "Ojtlatokani",
                "profile_low_conn" to "Amo onka internet",
                "reading_level_label" to "Tlapohualiztli:",
                "level_simple" to "Tlawel yemanki",
                "level_standard" to "Kwalli",
                "suggested_questions_title" to "Tlajtlanilistli:",
                "q_pension" to "¿Tlen moneki ipampa pensión de wewentsin?",
                "q_scholarship" to "¿Kenin niktlahtlanis beca ipampa momachtijketl?",
                "q_health" to "¿Kampa onka pajtli tlen amo patiyoh?",
                "q_migrant" to "¿Kenin nikchiwas permiso de tekitl iwan CURP?",
                "input_placeholder" to "Xikihkuilo nikan motlajtlanil...",
                "btn_listen" to "Xikkaki tlanankililli",
                "btn_save" to "Xikajoku amatl",
                "btn_official_link" to "Xiyawi oficina",
                "reader_title" to "Xiktlachili se amatl",
                "btn_take_photo" to "Xikixkopina iwan Xikmelawa",
                "btn_choose_gallery" to "Xikpejpena foto",
                "btn_demo_prescription" to "Noso xikita se amatl de ejemplo:",
                "summary_title" to "Tlamelawalistli yemanki",
                "steps_title" to "Tlen nikchiwas",
                "saved_title" to "Noamamej ajoktokej",
                "saved_empty" to "Ahmo catqui amatl.",
                "btn_delete" to "Xiquixpolo",
                "offline_badge" to "Ahmo onka internet",
                "help_dialog_title" to "Palehuilistli",
                "help_dialog_content" to "GuíaFácil mitzpalehuia:\n1. Xitlahto motlahtol.\n2. Xikixkopina amatl.\n3. Xikajoku ipampa amo onka internet.",
                "btn_close" to "Xictzacua",
                "btn_consult" to "Xiktitlanili",
                "voice_prompt" to "Tlahto..."
            )
        }

        if (isZoque) {
            return mapOf(
                "app_title" to "GuíaFácil",
                "app_subtitle" to "Tzame's Oportunidad",
                "welcome_title" to "Wüjü minba GuíaFácil-omo",
                "custom_lang_placeholder" to "¿Ja' mi mätzäjkä mi tzame? Jayä yä'ki",
                "btn_continue" to "Pämi",
                "home_header" to "Wüjü, ¿ti-omo täj tzokoyä yäti?",
                "card_assistant_title" to "Tzame's GuíaFácil-ji'ŋ",
                "card_assistant_desc" to "Wäkä mi tzame-ji'ŋ o jayä",
                "card_reader_title" to "Isä tumä toto",
                "card_reader_desc" to "Ch'äkä foto tzoy o toto-omo",
                "card_saved_title" to "Än Toto Nätzäjkuyä",
                "card_saved_desc" to "Isä mi toto ja' ityä internet",
                "footer_text" to "Mi tzame-omo. Wä'ä jama mi-ji'ŋ.",
                "btn_need_help" to "¿Mi sunba tzokoyä?",
                "assistant_title" to "Tzame'sdäm wüjü-omo",
                "profile_label" to "Mi pät:",
                "profile_elderly" to "Pä'nis / Yomo'is",
                "profile_student" to "Angmayobä",
                "profile_migrant" to "Wityobä",
                "profile_low_conn" to "Ja' ityä internet",
                "reading_level_label" to "Nivel:",
                "level_simple" to "Wüjü matonä",
                "level_standard" to "Wüjü",
                "suggested_questions_title" to "Wäkuyä:",
                "q_pension" to "¿Ti sunba pensión de pä'nis-omo?",
                "q_scholarship" to "¿Jutä wäkäpa beca angmayobä-omo?",
                "q_health" to "¿Jutä ityä tzoy ja' mayäpa?",
                "q_migrant" to "¿Jutä täjkäpa permiso de yoskuy y CURP?",
                "input_placeholder" to "Jayä yä'ki mi wäkuyä...",
                "btn_listen" to "Matonä tzame",
                "btn_save" to "Nätzäjkä toto",
                "btn_official_link" to "Mängä oficina-omo",
                "reader_title" to "Isä tumä toto",
                "btn_take_photo" to "Ch'äkä Foto y Tzame's",
                "btn_choose_gallery" to "Yajk'ä foto",
                "btn_demo_prescription" to "O isä tumä toto de ejemplo:",
                "summary_title" to "Tzame wüjü matonä",
                "steps_title" to "Ti än täjkäpa",
                "saved_title" to "Än toto nätzäjkuyä",
                "saved_empty" to "Ka'i toto.",
                "btn_delete" to "Jop'o",
                "offline_badge" to "Ja' ityä internet",
                "help_dialog_title" to "Tzokoyä",
                "help_dialog_content" to "GuíaFácil:\n1. Tzame's.\n2. Ch'äkä foto.\n3. Nätzäjkä ja' ityä internet.",
                "btn_close" to "Close",
                "btn_consult" to "Kächä",
                "voice_prompt" to "Tzame's..."
            )
        }

        if (isHaitian) {
            return mapOf(
                "app_title" to "GuíaFácil",
                "app_subtitle" to "Ponte Opòtunite",
                "welcome_title" to "Byenvini nan GuíaFácil",
                "custom_lang_placeholder" to "Ou pa jwenn lang ou? Ekri li isit la",
                "btn_continue" to "Kontinye",
                "home_header" to "Bonjou, kòman mwen ka ede w jodi a?",
                "card_assistant_title" to "Pale ak GuíaFácil",
                "card_assistant_desc" to "Mande ak vwa ou oswa ekri",
                "card_reader_title" to "Li yon Dokiman",
                "card_reader_desc" to "Pran foto preskripsyon oswa papye",
                "card_saved_title" to "Demach Mwen Sere",
                "card_saved_desc" to "Gade gid ou sere san entènèt",
                "footer_text" to "Nan ritm pa w. Toujou avèk ou.",
                "btn_need_help" to "Ou bezwen èd?",
                "assistant_title" to "Ann pale ak kalm",
                "profile_label" to "Pwofil ou:",
                "profile_elderly" to "Granmoun aje",
                "profile_student" to "Etidyan 1ye jen.",
                "profile_migrant" to "Moun migran",
                "profile_low_conn" to "Pa gen entènèt",
                "reading_level_label" to "Nivo lekti:",
                "level_simple" to "Trè senp",
                "level_standard" to "Nòmal",
                "suggested_questions_title" to "Kesyon komen:",
                "q_pension" to "Kisa mwen bezwen pou mande pansyon granmoun aje?",
                "q_scholarship" to "Kòman pou m mande yon bous etid inivèsitè?",
                "q_health" to "Ki kote mwen ka jwenn swen sante gratis?",
                "q_migrant" to "Kòman pou m fè pèmi travay oswa CURP kòm migran?",
                "input_placeholder" to "Ekri kesyon ou isit la...",
                "btn_listen" to "Koute repons lan",
                "btn_save" to "Sere lis la",
                "btn_official_link" to "Ale nan biwo ofisyèl",
                "reader_title" to "Li yon dokiman",
                "btn_take_photo" to "Pran Foto epi Eksplike",
                "btn_choose_gallery" to "Chwazi foto nan galri",
                "btn_demo_prescription" to "Oswa eseye ak yon dokiman egzanp:",
                "summary_title" to "Rezime senp",
                "steps_title" to "Pwochen etap mwen yo",
                "saved_title" to "Demach mwen sere",
                "saved_empty" to "Ou pa gen pwosedi sove ankò.",
                "btn_delete" to "Efase",
                "offline_badge" to "Sere san entènèt",
                "help_dialog_title" to "Èd",
                "help_dialog_content" to "GuíaFácil ede w:\n1. Pale nan lang ou.\n2. Pran foto dokiman.\n3. Sere etap yo san entènèt.",
                "btn_close" to "Fèmen",
                "btn_consult" to "Voye",
                "voice_prompt" to "Pale..."
            )
        }

        if (isEnglish) {
            return mapOf(
                "app_title" to "GuíaFácil",
                "app_subtitle" to "Bridges of Opportunity",
                "welcome_title" to "Welcome to GuíaFácil",
                "custom_lang_placeholder" to "Can't find your language? Write it here",
                "btn_continue" to "Continue",
                "home_header" to "Hello, how can I help you today?",
                "card_assistant_title" to "Talk with GuíaFácil",
                "card_assistant_desc" to "Ask with your voice or type",
                "card_reader_title" to "Read a Document",
                "card_reader_desc" to "Take a photo of prescriptions or forms",
                "card_saved_title" to "My Saved Procedures",
                "card_saved_desc" to "Check your saved guides offline",
                "footer_text" to "At your own pace. Always with you.",
                "btn_need_help" to "Need help?",
                "assistant_title" to "Let's talk calmly",
                "profile_label" to "Your profile:",
                "profile_elderly" to "Older adult",
                "profile_student" to "1st-gen student",
                "profile_migrant" to "Migrant person",
                "profile_low_conn" to "Low connectivity",
                "reading_level_label" to "Reading level:",
                "level_simple" to "Very simple",
                "level_standard" to "Standard",
                "suggested_questions_title" to "Common questions:",
                "q_pension" to "What do I need to apply for the older adult pension?",
                "q_scholarship" to "How do I apply for a university scholarship?",
                "q_health" to "Where can I get free medical care without insurance?",
                "q_migrant" to "How do I get a work permit or CURP as a migrant?",
                "input_placeholder" to "Write your question here...",
                "btn_listen" to "Listen to answer",
                "btn_save" to "Save list",
                "btn_official_link" to "Go to official office",
                "reader_title" to "Read a document",
                "btn_take_photo" to "Take Photo & Explain",
                "btn_choose_gallery" to "Choose photo from gallery",
                "btn_demo_prescription" to "Or try with a sample document:",
                "summary_title" to "Simple summary",
                "steps_title" to "My next steps",
                "saved_title" to "My saved procedures",
                "saved_empty" to "No saved procedures yet.",
                "btn_delete" to "Delete",
                "offline_badge" to "Available offline",
                "help_dialog_title" to "Quick Guide & Support",
                "help_dialog_content" to "GuíaFácil helps you:\n1. Ask in your language.\n2. Photo documents to understand them.\n3. Save steps to consult offline.",
                "btn_close" to "Close",
                "btn_consult" to "Send",
                "voice_prompt" to "Speak..."
            )
        }

        // Para cualquier otro idioma o lengua desconocida, devolvemos EXACTAMENTE el español, SIN paréntesis ni alterations.
        return defaultSpanishDictionary.mapValues { (key, value) ->
            if (key == "app_subtitle") language
            else value
        }
    }

    private fun getFallbackAssistantResponse(question: String): AssistantResponse {
        val isPension = question.contains("Pensión", ignoreCase = true) || question.contains("Bienestar", ignoreCase = true)
        val isBeca = question.contains("Beca", ignoreCase = true) || question.contains("estudiante", ignoreCase = true)
        val isSalud = question.contains("médica", ignoreCase = true) || question.contains("salud", ignoreCase = true) || question.contains("seguro", ignoreCase = true)
        val isMigrante = question.contains("migra", ignoreCase = true) || question.contains("refugio", ignoreCase = true) || question.contains("regulariz", ignoreCase = true) || question.contains("curp", ignoreCase = true)

        val summary: String
        val steps: List<String>
        val officialInfo: String

        when {
            isMigrante -> {
                summary = "Trámites de regularización, CURP temporal y empleo formal para personas migrantes y en movilidad en México."
                steps = listOf(
                    "1. Acude a la oficina de COMAR o Instituto Nacional de Migración (INM) más cercana.",
                    "2. Solicita tu constancia de trámite o Tarjeta de Visitante por Razones Humanitarias.",
                    "3. Obtén tu CURP temporal gratuita para acceder a servicios de salud y trabajo formal."
                )
                officialInfo = "📍 Oficina COMAR / INM | 📞 Línea de Orientación Migrante: 800 004 6264"
            }
            isPension -> {
                summary = "La Pensión para el Bienestar de los Adultos Mayores es un apoyo económico universal entregado bimestralmente."
                steps = listOf(
                    "1. Reúne tu identificación oficial (INE), CURP y comprobante de domicilio.",
                    "2. Ubica tu Módulo del Bienestar más cercano.",
                    "3. Entrega tu documentación para recibir tu tarjeta del Banco del Bienestar."
                )
                officialInfo = "📍 Módulo del Bienestar | 📞 Línea del Bienestar: 800-639-4264"
            }
            isBeca -> {
                summary = "Las Becas de Educación Superior apoyan a estudiantes de primera generación universitaria."
                steps = listOf(
                    "1. Solicita la constancia de estudios vigente en tu universidad.",
                    "2. Regístrate en la plataforma oficial de becas.",
                    "3. Captura tu cuenta bancaria para recibir el depósito directo."
                )
                officialInfo = "📍 Oficina de Becas | 📞 Tel: 55-1162-0300"
            }
            isSalud -> {
                summary = "Toda persona tiene derecho a recibir atención médica y medicamentos gratuitos en IMSS-Bienestar."
                steps = listOf(
                    "1. Acude al Centro de Salud público más cercano.",
                    "2. Presenta identificación y CURP para tu expediente clínico.",
                    "3. Recibe la consulta médica y tus medicamentos sin costo."
                )
                officialInfo = "📍 Centro de Salud IMSS-Bienestar | 📞 Salud: 800-767-8527"
            }
            else -> {
                summary = "GuíaFácil ha resumido los pasos para tu trámite: '$question'."
                steps = listOf(
                    "1. Junta tu identificación (INE), CURP y comprobante.",
                    "2. Acude a la ventanilla oficial de atención.",
                    "3. Guarda tu comprobante de trámite."
                )
                officialInfo = "📍 Ventanilla Única de Atención | 📞 Línea directa: 800-00-4842"
            }
        }

        return AssistantResponse(
            question = question,
            summaryText = summary,
            steps = steps,
            officialInfo = officialInfo
        )
    }

    private fun getFallbackDocumentResult(): DocumentAnalysisResult {
        return DocumentAnalysisResult(
            summary = "Indicación Médica / Documento Oficial: Presenta este documento en ventanilla o farmacia. Indica tomar el medicamento con alimentos cada 8 horas por 7 días.",
            steps = listOf(
                CheckableStep(1, "Tomar 1 tableta indicada cada 8 horas después de alimentos.", false),
                CheckableStep(2, "Mantener el tratamiento completo durante 7 días consecutivos.", false),
                CheckableStep(3, "Presentar la receta en la farmacia comunitaria para resurtir.", false)
            )
        )
    }
}
