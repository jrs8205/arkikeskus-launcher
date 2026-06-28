package org.arkikeskus.launcher.feature.backup.drive

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

data class DriveFile(val id: String, val name: String, val modifiedTime: String)

/** Minimal Drive v3 REST client scoped to appDataFolder. [token] is a fresh OAuth access token. */
class DriveRestClient(
    private val token: String,
    private val http: OkHttpClient = OkHttpClient(),
) {
    private fun authed(b: Request.Builder) = b.header("Authorization", "Bearer $token")

    fun upload(name: String, json: String) {
        val meta = JSONObject().put("name", name).put("parents", listOf("appDataFolder").let {
            org.json.JSONArray().apply { put("appDataFolder") }
        })
        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(meta.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(json.toRequestBody("application/json".toMediaType()))
            .build()
        val req = authed(Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .post(body)).build()
        http.newCall(req).execute().use { if (!it.isSuccessful) throw IOException("upload ${it.code}") }
    }

    fun list(): List<DriveFile> {
        val req = authed(Request.Builder().url(
            "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder" +
                "&orderBy=modifiedTime%20desc&fields=files(id,name,modifiedTime)",
        )).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("list ${resp.code}")
            val files = JSONObject(resp.body!!.string()).getJSONArray("files")
            return (0 until files.length()).map { i ->
                val o = files.getJSONObject(i)
                DriveFile(o.getString("id"), o.getString("name"), o.optString("modifiedTime"))
            }
        }
    }

    fun download(id: String): String {
        val req = authed(Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$id?alt=media")).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("download ${resp.code}")
            return resp.body!!.string()
        }
    }

    fun delete(id: String) {
        val req = authed(Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$id").delete()).build()
        http.newCall(req).execute().use { if (!it.isSuccessful) throw IOException("delete ${it.code}") }
    }

    fun pruneToNewest(keep: Int) {
        list().drop(keep).forEach { delete(it.id) }
    }
}
