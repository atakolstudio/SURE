package com.atakolstudio.sure.data.ir

/**
 * Uzaktan kumanda ekranında yer alabilecek tüm tuşlar.
 * Her marka kod tablosu, bu tuşlardan desteklediklerini eşler; eksik olanlar
 * arayüzde otomatik olarak devre dışı/gizli gösterilir.
 */
enum class RemoteButton {
    POWER,
    VOLUME_UP, VOLUME_DOWN, MUTE,
    CHANNEL_UP, CHANNEL_DOWN,
    UP, DOWN, LEFT, RIGHT, OK,
    MENU, BACK, HOME, INPUT, SETTINGS,
    RED, GREEN, YELLOW, BLUE,
    NUM_0, NUM_1, NUM_2, NUM_3, NUM_4,
    NUM_5, NUM_6, NUM_7, NUM_8, NUM_9,
    PLAY_PAUSE, REWIND, FAST_FORWARD, STOP
}
