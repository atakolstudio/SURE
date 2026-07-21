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
 * `ConsumerIrManager` uzerinden gercek kizilotesi sinyal gonderimini yonetir.
 * Cihazda IR donanimi yoksa (`hasIrEmitter() == false`), bunu zarifce raporlar;
 * uygulama cokmez.
 */
@Singleton
class IrTransmitter @Inject constructor(
    @ApplicationContext context: Context
) {
    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val hasIrEmitter: Boolean
        get() = irManager?.hasIrEmitter() == true

    /** Bilinen bir marka + tus kombinasyonunu gonderir. */
    fun send(brand: BrandIrCodeSet, button: RemoteButton): IrTransmitResult {
        val irCommand = BrandIrDatabase.toIrCommand(brand, button)
            ?: return IrTransmitResult.ButtonNotMapped
        return sendRaw(irCommand)
    }

    /**
     * Ham bir IR komutunu (protokol + adres + komut) dogrudan gonderir.
     * "Manuel Bul" (kod tarama / elle kod girme) akislari bunu kullanir; henuz
     * bir markaya/tusa baglanmamis, test amacli komutlar icin idealdir.
     */
    fun sendRaw(command: IrCommand): IrTransmitResult {
        val manager = irManager ?: return IrTransmitResult.NoIrHardware
        if (!manager.hasIrEmitter()) return IrTransmitResult.NoIrHardware

        return try {
            val pattern = IrCodeEncoder.encode(command)
            manager.transmit(command.protocol.carrierFrequencyHz, pattern)
            IrTransmitResult.Success
        } catch (e: UnsupportedOperationException) {
            IrTransmitResult.FrequencyNotSupported
        } catch (e: Exception) {
            IrTransmitResult.Error(e.message ?: "Bilinmeyen IR gonderim hatasi")
        }
    }
}
