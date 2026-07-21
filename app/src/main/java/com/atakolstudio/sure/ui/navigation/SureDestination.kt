package com.atakolstudio.sure.ui.navigation

/**
 * Uygulamanın tüm ekranlarını ve rota (route) tanımlarını tek yerde toplar.
 * Argümanlı rotalar için yardımcı `createRoute` fonksiyonları sağlanır.
 */
sealed class SureDestination(val route: String) {

    object Devices : SureDestination("devices")

    object BrandSelection : SureDestination("brand_selection/{deviceType}/{connectionType}") {
        fun createRoute(deviceType: String, connectionType: String) =
            "brand_selection/$deviceType/$connectionType"
    }

    object DeviceTypeSelection : SureDestination("device_type_selection")

    object ManualSearch : SureDestination("manual_search/{deviceType}/{connectionType}") {
        fun createRoute(deviceType: String, connectionType: String) =
            "manual_search/$deviceType/$connectionType"
    }

    object ConnectionTypeSelection : SureDestination("connection_type_selection/{deviceType}") {
        fun createRoute(deviceType: String) = "connection_type_selection/$deviceType"
    }

    object Remote : SureDestination(
        "remote/{savedDeviceId}?brandKey={brandKey}&deviceType={deviceType}&connectionType={connectionType}"
    ) {
        /** Yeni (henüz kaydedilmemiş) bir cihaz için ilk kurulum akışı. */
        fun createRouteForSetup(brandKey: String, deviceType: String, connectionType: String) =
            "remote/-1?brandKey=$brandKey&deviceType=$deviceType&connectionType=$connectionType"

        /** Kayıtlı bir cihazı açmak için. */
        fun createRouteForSavedDevice(savedDeviceId: Long) =
            "remote/$savedDeviceId"
    }
}
