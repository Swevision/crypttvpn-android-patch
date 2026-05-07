package com.crypttvpn.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crypttvpn.data.AccountRepository
import com.crypttvpn.data.remote.AccountDto
import com.crypttvpn.data.remote.AppStatusDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AccountUiState {
    data object Loading : AccountUiState
    data class Success(val data: AccountDto, val banner: AppStatusDto?) : AccountUiState
    data class Error(val message: String) : AccountUiState
    data object NotActivated : AccountUiState
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repo: AccountRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = AccountUiState.Loading
        viewModelScope.launch {
            val accountResult = repo.fetchAccount()
            val bannerResult = repo.fetchStatus()
            val banner = bannerResult.getOrNull()?.takeIf { it.active }

            accountResult.fold(
                onSuccess = { _state.value = AccountUiState.Success(it, banner) },
                onFailure = { e ->
                    val msg = e.message.orEmpty()
                    _state.value = when {
                        msg.contains("401") || msg.contains("unauthorized", ignoreCase = true) ->
                            AccountUiState.NotActivated
                        else -> AccountUiState.Error(
                            msg.ifBlank { "Не удалось загрузить кабинет" }
                        )
                    }
                },
            )
        }
    }
}
