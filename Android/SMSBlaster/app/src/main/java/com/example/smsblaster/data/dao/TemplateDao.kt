package com.example.smsblaster.data.dao

import androidx.room.*
import com.example.smsblaster.data.model.Template
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY updatedAt DESC")
    fun getAllTemplates(): Flow<List<Template>>
    
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): Template?
    
    @Query("SELECT * FROM templates WHERE name LIKE :query OR content LIKE :query")
    fun searchTemplates(query: String): Flow<List<Template>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: Template): Long
    
    @Update
    suspend fun updateTemplate(template: Template)
    
    @Delete
    suspend fun deleteTemplate(template: Template)
    
    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)
    
    @Query("SELECT COUNT(*) FROM templates")
    fun getTemplateCount(): Flow<Int>
}
