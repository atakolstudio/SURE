package com.atakolstudio.sure.di

/**
 * NOT: IrTransmitter, kendi `@Inject constructor` ve `@ApplicationContext` niteleyicisi
 * sayesinde Hilt tarafından otomatik olarak sağlanır (bkz. data/ir/IrTransmitter.kt).
 * Bu yüzden burada ayrıca bir @Provides metoduna gerek yoktur — aksi halde
 * "IrTransmitter is bound multiple times" hatası alınır.
 *
 * Bu dosya, ileride Context'e bağlı başka singleton'lar eklemek için bir yer tutucu
 * olarak bırakılmıştır.
 */
