package com.atakolstudio.sure.data.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class IrTransmitResult {
    object Success : IrTransmitResult()
    object NoIrHardware : IrTransmitResult()
    object FrequencyNotSupported : IrTransmitResult()
    object ButtonNotMapped : IrTransmitResult()
    data class Error(val message: String) : IrTransmitResult()
}

/**
 * `ConsumerIrManager` üzerinden gerçek kızılötesi sinyal gönderimini yönetir.
 * Cihazda IR donanımı yoksa (`hasIrEmitter() == false`), bunu zarifçe raporlar;
 * uygulama çökmez.
 */
@Singleton
class IrTransmitter @Inject constructor(
    @ApplicationContext context: Context
) {
    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val hasIrEmitter: Boolean
        get() = irManager?.hasIrEmitter() == true

    fun send(brand: BrandIrCodeSet, button: RemoteButton): IrTransmitResult {
        val manager = irManager ?: return IrTransmitResult.NoIrHardware
        if (!manager.hasIrEmitter()) return IrTransmitResult.NoIrHardware

        val irCommand = BrandIrDatabase.toIrCommand(brand, button)
            ?: return IrTransmitResult.ButtonNotMapped

        return try {
            val pattern = IrCodeEncoder.encode(irCommand)
            manager.transmit(brand.protocol.carrierFrequencyHz, pattern)
            IrTransmitResult.Success
        } catch (e: UnsupportedOperationException) {
            IrTransmitResult.FrequencyNotSupported
        } catch (e: Exception) {
            IrTransmitResult.Error(e.message ?: "Bilinmeyen IR gönderim hatası")
        }
    }
}
