package com.specialschool.schoolapp.data.userevent

import androidx.annotation.WorkerThread
import com.specialschool.schoolapp.data.SchoolRepository
import com.specialschool.schoolapp.domain.bookmark.StarUpdatedStatus
import com.specialschool.schoolapp.model.School
import com.specialschool.schoolapp.model.UserEvent
import com.specialschool.schoolapp.model.UserItem
import com.specialschool.schoolapp.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface SchoolAndUserItemRepository {

    fun getObservableUserItems(
        userId: String?
    ): Flow<Result<ObservableUserItems>>

    fun getObservableUserItem(
        userId: String?,
        itemId: String
    ): Flow<Result<UserItem>>

    fun getUserEvents(userId: String?): List<UserEvent>

    suspend fun starEvent(userId: String, userEvent: UserEvent): Result<StarUpdatedStatus>

    fun getUserItem(userId: String, itemId: String): UserItem
}

// TODO: change name?
data class ObservableUserItems(
    val userItems: List<UserItem>
)

/**
 * 학교 데이터와 User data를 불러오는 repository
 */
@ExperimentalCoroutinesApi
class DefaultSchoolAndUserItemRepository @Inject constructor(
    private val userEventDataSource: UserEventDataSource,
    private val schoolRepository: SchoolRepository
) : SchoolAndUserItemRepository {

    /**
     * 학교 데이터 목록과 User event 목록을 각각 불러와 병합해서 반환한다.
     */
    override fun getObservableUserItems(userId: String?): Flow<Result<ObservableUserItems>> {
        return flow {
            emit(Result.Loading)
            if (userId == null) {
                val schools = schoolRepository.getSchoolList()
                val userItems = mergeUserDataAndSchools(emptyList(), schools)
                emit(Result.Success(ObservableUserItems(userItems)))
            } else {
                emitAll(userEventDataSource.getObservableUserEvents(userId).map { events ->
                    val schools = schoolRepository.getSchoolList()
                    val userItems = mergeUserDataAndSchools(events, schools)
                    Result.Success(
                        ObservableUserItems(userItems)
                    )
                })
            }
        }
    }

    /**
     * 특정 학교 데이터와 User event 데이터를 병합해서 반환한다.
     */
    override fun getObservableUserItem(userId: String?, itemId: String): Flow<Result<UserItem>> {
        if (userId == null) {
            val school = schoolRepository.getSchool(itemId)
            return flow {
                emit(
                    Result.Success(UserItem(school, createDefaultUserEvent(school)))
                )
            }
        }

        return userEventDataSource.getObservableUserEvent(userId, itemId).map { result ->
            val school = schoolRepository.getSchool(itemId)
            Result.Success(UserItem(school, result))
        }
    }

    // TODO: unused method
    override fun getUserEvents(userId: String?): List<UserEvent> {
        return userEventDataSource.getUserEvents(userId ?: "")
    }

    /**
     * @see [FirestoreUserEventDataSource.starEvent]
     */
    override suspend fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): Result<StarUpdatedStatus> = userEventDataSource.starEvent(userId, userEvent)

    // TODO: unused method
    override fun getUserItem(userId: String, itemId: String): UserItem {
        val school = schoolRepository.getSchool(itemId)
        val userEvent = userEventDataSource.getUserEvent(userId, itemId)
            ?: throw Exception("UserEvent not found")

        return UserItem(
            school = school,
            userEvent = userEvent
        )
    }

    private fun createDefaultUserEvent(school: School): UserEvent {
        return UserEvent(id = school.id)
    }

    // User data와 학교 데이터 목록을 병합한다.
    @WorkerThread
    private fun mergeUserDataAndSchools(
        userData: List<UserEvent>,
        schools: List<School>
    ): List<UserItem> {
        if (userData.isEmpty()) {
            return schools.map { UserItem(it, createDefaultUserEvent(it)) }
        }

        val eventIdToUserEvent = userData.associateBy { it.id }
        return schools.map {
            UserItem(it, eventIdToUserEvent[it.id] ?: createDefaultUserEvent(it))
        }
    }
}
