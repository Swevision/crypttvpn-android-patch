package com.crypttvpn.domain

/**
 * Пример ViewModel для активации. Адаптируй под свою архитектуру.
 */
/*
@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val api: CrypttVpnApi,
    private val storage: SecureStorage,
) : ViewModel() {

    private val _state = MutableStateFlow<ActivationState>(ActivationState.Idle)
    val state: StateFlow<ActivationState> = _state

    fun activate(key: String) {
        viewModelScope.launch {
            _state.value = ActivationState.Loading
            try {
                val response = api.activate(
                    ActivateRequest(
                        key = key,
                        deviceId = storage.deviceId,
                        deviceInfo = DeviceInfo(
                            osVersion = Build.VERSION.RELEASE,
                            appVersion = BuildConfig.VERSION_NAME,
                            model = Build.MODEL,
                        ),
                    ),
                )
                storage.token = response.token
                storage.subscriptionId = response.subscription.id
                _state.value = ActivationState.Success(response.subscription)
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string().orEmpty()
                _state.value = when (e.code()) {
                    404 -> ActivationState.Error("Ключ не найден")
                    409 -> ActivationState.Error("Ключ уже использован")
                    410 -> ActivationState.Error("Ключ истёк")
                    else -> ActivationState.Error("Ошибка активации: ${e.code()}")
                }
            } catch (e: IOException) {
                _state.value = ActivationState.Error("Нет связи с сервером")
            }
        }
    }
}

sealed interface ActivationState {
    data object Idle : ActivationState
    data object Loading : ActivationState
    data class Success(val subscription: SubscriptionDto) : ActivationState
    data class Error(val message: String) : ActivationState
}
*/
