package com.example.conneqt

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

object LocalStorage {

    private const val PREFS_NAME  = "conneqt_prefs"

    // ── Each professor gets their own key using their Firebase UID ──
    private fun classesKey(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: "guest"
        return "classes_$uid"
    }

    // ── Save a new class (with its students embedded) ───────────────
    fun saveClass(context: Context, classModel: ClassModel, students: List<StudentModel>) {
        val prefs   = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val classes = getAllClassesJson(context)

        val classJson = JSONObject().apply {
            put("id",           classModel.id)
            put("name",         classModel.name)
            put("studentCount", students.size)
            put("uploadDate",   classModel.uploadDate)
            put("emoji",        classModel.emoji)
            put("isActive",     classModel.isActive)

            val studentsArray = JSONArray()
            for (s in students) {
                studentsArray.put(JSONObject().apply {
                    put("id",        s.id)
                    put("name",      s.name)
                    put("studentNo", s.studentNo)
                    put("phone",     s.phone)
                    put("email",     s.email)
                })
            }
            put("students", studentsArray)
        }

        classes.put(classJson)
        prefs.edit().putString(classesKey(), classes.toString()).apply()
    }

    // ── Get all classes for the current professor ───────────────────
    fun getAllClasses(context: Context): MutableList<ClassModel> {
        val classes = getAllClassesJson(context)
        val result  = mutableListOf<ClassModel>()

        for (i in 0 until classes.length()) {
            val obj = classes.getJSONObject(i)
            result.add(
                ClassModel(
                    id           = obj.getString("id"),
                    name         = obj.getString("name"),
                    studentCount = obj.optInt("studentCount", 0),
                    uploadDate   = obj.optString("uploadDate", ""),
                    emoji        = obj.optString("emoji", "📘"),
                    isActive     = obj.optBoolean("isActive", true)
                )
            )
        }
        return result
    }

    // ── Get students for a specific class ───────────────────────────
    fun getStudents(context: Context, classId: String): List<StudentModel> {
        val classes  = getAllClassesJson(context)
        val students = mutableListOf<StudentModel>()

        for (i in 0 until classes.length()) {
            val obj = classes.getJSONObject(i)
            if (obj.getString("id") == classId) {
                val arr = obj.optJSONArray("students") ?: break
                for (j in 0 until arr.length()) {
                    val s = arr.getJSONObject(j)
                    students.add(
                        StudentModel(
                            id        = s.optString("id", ""),
                            name      = s.optString("name", ""),
                            studentNo = s.optString("studentNo", ""),
                            phone     = s.optString("phone", ""),
                            email     = s.optString("email", "")
                        )
                    )
                }
                break
            }
        }
        return students
    }

    // ── Delete a class by ID ────────────────────────────────────────
    fun deleteClass(context: Context, classId: String) {
        val prefs      = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val classes    = getAllClassesJson(context)
        val newClasses = JSONArray()

        for (i in 0 until classes.length()) {
            val obj = classes.getJSONObject(i)
            if (obj.getString("id") != classId) {
                newClasses.put(obj)
            }
        }
        prefs.edit().putString(classesKey(), newClasses.toString()).apply()
    }

    // ── Internal: get raw JSON array for current professor ──────────
    private fun getAllClassesJson(context: Context): JSONArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(classesKey(), "[]") ?: "[]"
        return try {
            JSONArray(json)
        } catch (e: Exception) {
            JSONArray()
        }
    }
}