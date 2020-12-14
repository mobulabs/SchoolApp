package com.specialschool.schoolapp.ui.search

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.specialschool.schoolapp.data.signin.AuthenticatedUserInfo
import com.specialschool.schoolapp.domain.bookmark.StarEventParameter
import com.specialschool.schoolapp.domain.bookmark.StarEventUseCase
import com.specialschool.schoolapp.domain.schooldata.LoadUserItemsUseCase
import com.specialschool.schoolapp.domain.schooldata.RefreshSchoolDataUseCase
import com.specialschool.schoolapp.domain.search.SearchParameter
import com.specialschool.schoolapp.domain.search.SearchUseCase
import com.specialschool.schoolapp.model.UserItem
import com.specialschool.schoolapp.ui.event.EventActions
import com.specialschool.schoolapp.ui.signin.SignInViewModelDelegate
import com.specialschool.schoolapp.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class SearchViewModel @ViewModelInject constructor(
    signInViewModelDelegate: SignInViewModelDelegate,
    private val searchUseCase: SearchUseCase,
    private val loadUserItemsUseCase: LoadUserItemsUseCase,
    private val starEventUseCase: StarEventUseCase,
    private val refreshSchoolDataUseCase: RefreshSchoolDataUseCase
) : ViewModel(),
    EventActions,
    SignInViewModelDelegate by signInViewModelDelegate {

    private var loadUserItemsJob: Job? = null

    private var searchJob: Job? = null

    private val _searchResults = MediatorLiveData<List<UserItem>>()
    val searchResults: LiveData<List<UserItem>> = _searchResults

    private val _navigateToSchoolDetailAction = MutableLiveData<Event<String>>()
    val navigateToSchoolDetailAction: LiveData<Event<String>> = _navigateToSchoolDetailAction

    private val _navigateToSignInDialogAction = MutableLiveData<Event<Unit>>()
    val navigateToSignInDialogAction: LiveData<Event<Unit>> = _navigateToSignInDialogAction

    private var textQuery = ""

    private val currentUserObserver = Observer<AuthenticatedUserInfo?> {
        executeSearch()
    }

    init {
        viewModelScope.launch {
            refreshSchoolDataUseCase(Any())
            currentFirebaseUser.collect {
                refreshUserItems()
            }
        }
        currentUserInfo.observeForever(currentUserObserver)
    }

    fun onSearchQueryChanged(query: String) {
        val newQuery = query.trim().takeIf { it.length >= 2 } ?: ""
        if (textQuery != newQuery) {
            textQuery = newQuery
            executeSearch()
        }
    }

    private fun executeSearch() {
        searchJob?.cancel()

        if (textQuery.isEmpty()) {
            clearSearchResults()
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            searchUseCase(
                SearchParameter(getUserId(), textQuery)
            ).collect {
                processSearchResult(it)
            }
        }
    }

    private fun processSearchResult(result: Result<List<UserItem>>) {
        if (result is Result.Loading) {
            return // avoids UI flickering
        }
        val userItems = result.successOr(emptyList())
        _searchResults.value = userItems
    }

    private fun clearSearchResults() {
        refreshUserItems()
        //_searchResults.value = emptyList()
    }

    private fun refreshUserItems() {
        loadUserItemsJob.cancelIfActive()

        loadUserItemsJob = viewModelScope.launch {
            loadUserItemsUseCase(getUserId()).collect {
                _searchResults.value = it.data
            }
        }
    }

    override fun openItemDetail(id: String) {
        _navigateToSchoolDetailAction.value = Event(id)
    }

    override fun onStarClicked(userItem: UserItem) {
        if (!isSignedIn()) {
            _navigateToSignInDialogAction.value = Event(Unit)
            return
        }
        val newIsStarredState = !userItem.userEvent.isStarred

        viewModelScope.launch {
            getUserId()?.let {
                val result = starEventUseCase(
                    StarEventParameter(
                        it, userItem.copy(
                            userEvent = userItem.userEvent.copy(isStarred = newIsStarredState)
                        )
                    )
                )
                // Show an error message if a star request fails
                if (result is Result.Error) {

                }
            }
        }
    }

    private val _test1 = MutableLiveData<String>().apply {
        value = "검색"
    }
    val test: LiveData<String> = _test1
}
