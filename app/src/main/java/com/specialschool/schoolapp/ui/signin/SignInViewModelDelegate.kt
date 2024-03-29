package com.specialschool.schoolapp.ui.signin

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.specialschool.schoolapp.data.signin.AuthenticatedUserInfo
import com.specialschool.schoolapp.di.MainDispatcher
import com.specialschool.schoolapp.domain.auth.ObserveUserAuthStateUseCase
import com.specialschool.schoolapp.util.Event
import com.specialschool.schoolapp.util.Result
import com.specialschool.schoolapp.util.Result.Error
import com.specialschool.schoolapp.util.Result.Success
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel에서 인증 기능을 구현하는 인터페이스, Delegation을 통한 확장을 사용한다.
 */
interface SignInViewModelDelegate {

    val currentFirebaseUser: Flow<Result<AuthenticatedUserInfo?>>

    val currentUserInfo: LiveData<AuthenticatedUserInfo?>

    val currentUserImageUri: LiveData<Uri?>

    val performSignInEvent: MutableLiveData<Event<SignInEvent>>

    suspend fun emitSignInRequest()

    suspend fun emitSignOutRequest()

    fun observeSignedInUser(): LiveData<Boolean>

    fun isSignedIn(): Boolean

    fun getUserId(): String?
}

enum class SignInEvent {
    REQUEST_SIGN_IN, REQUEST_SIGN_OUT
}

/**
 * Firebase의 인증 기능을 사용하는 SignInViewModelDelegate 구현 클래스
 */
@ExperimentalCoroutinesApi
internal class FirebaseSignInViewModelDelegate @Inject constructor(
    private val observeUserAuthStateUseCase: ObserveUserAuthStateUseCase,
    @MainDispatcher private val dispatcher: CoroutineDispatcher
) : SignInViewModelDelegate {

    override val currentFirebaseUser: Flow<Result<AuthenticatedUserInfo?>>
        get() = observeUserAuthStateUseCase(Any()).map { result ->
            if (result is Success) {
                authenticatedUserInfo = result.data
                isSignedIn.value = result.data?.isSignedIn() ?: false
                _currentUserInfo.value = result.data
            } else if (result is Error) {
                Log.e("", "", result.exception)
            }
            result
        }

    private val _currentUserInfo = MutableLiveData<AuthenticatedUserInfo?>()
    override val currentUserInfo: LiveData<AuthenticatedUserInfo?>
        get() = _currentUserInfo

    override val currentUserImageUri: LiveData<Uri?> = currentUserInfo.map {
        it?.getPhotoUrl()
    }

    override val performSignInEvent = MutableLiveData<Event<SignInEvent>>()

    private val isSignedIn = MutableLiveData<Boolean>()

    private var authenticatedUserInfo: AuthenticatedUserInfo? = null

    override suspend fun emitSignInRequest() = withContext(dispatcher) {
        performSignInEvent.value = Event(SignInEvent.REQUEST_SIGN_IN)
    }

    override suspend fun emitSignOutRequest() = withContext(dispatcher) {
        performSignInEvent.value = Event(SignInEvent.REQUEST_SIGN_OUT)
    }

    override fun observeSignedInUser(): LiveData<Boolean> = isSignedIn

    override fun isSignedIn(): Boolean = isSignedIn.value == true

    override fun getUserId(): String? {
        return currentUserInfo.value?.getUid()
    }
}
