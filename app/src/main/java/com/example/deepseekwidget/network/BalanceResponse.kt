package com.example.deepseekwidget.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DeepSeek API 余额查询响应。
 *
 * 接口: GET https://api.deepseek.com/user/balance
 * 认证: Authorization: Bearer <API_KEY>
 *
 * 响应示例:
 * {
 *   "is_available": true,
 *   "balance_infos": [
 *     {
 *       "currency": "CNY",
 *       "total_balance": "110.00",
 *       "granted_balance": "10.00",
 *       "topped_up_balance": "100.00"
 *     }
 *   ]
 * }
 */
@Serializable
data class BalanceResponse(
    @SerialName("is_available")
    val isAvailable: Boolean = false,

    @SerialName("balance_infos")
    val balanceInfos: List<BalanceInfo> = emptyList()
)

@Serializable
data class BalanceInfo(
    val currency: String = "CNY",

    @SerialName("total_balance")
    val totalBalance: String = "0.00",

    @SerialName("granted_balance")
    val grantedBalance: String = "0.00",

    @SerialName("topped_up_balance")
    val toppedUpBalance: String = "0.00"
)
