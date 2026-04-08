package com.example.conneqt

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * LocalStorage — saves and loads classes + students using SharedPreferences.
 * No Firebase, no internet needed. Data persists across app restarts.
 *
 * Usage:
 *   LocalStorage.saveClass(context, classModel)
 *   LocalStorage.getAllClasses(context)
 *   LocalStorage.getStudents(context, classId)
 *   LocalStorage.deleteClass(context, classId)
 */
object LocalStorage {

    private const val PREFS_NAME   = "conneqt_prefs"
    private const val KEY_CLASSES  = "classes"

    // ── Save a new class (with its students embedded) ─────────────────
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

            // Embed students directly in the class JSON
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
        prefs.edit().putString(KEY_CLASSES, classes.toString()).commit()
    }

    // ── Get all classes (without student details) ──────────────────────
    fun getAllClasses(context: Context): MutableList<ClassModel> {
        val classes    = getAllClassesJson(context)
        val result     = mutableListOf<ClassModel>()

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

    // ── Get students for a specific class ──────────────────────────────
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

    // ── Delete a class by ID ───────────────────────────────────────────
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
        prefs.edit().putString(KEY_CLASSES, newClasses.toString()).commit()
    }

    // ── Internal: get raw JSON array of all classes ────────────────────
    private fun getAllClassesJson(context: Context): JSONArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_CLASSES, "[]") ?: "[]"
        return try {
            JSONArray(json)
        } catch (e: Exception) {
            JSONArray()
        }
    }
}