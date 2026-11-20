package com.example.bourget.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import com.example.bourget.data.TargetAppsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Android Auto Backup, gated by Bourget's own "백업" setting (off by default). The system, not
 * Bourget, uploads the backup to the user's Google account, so no internet permission is needed.
 * What is included is limited by res/xml/data_extraction_rules.xml and backup_rules.xml.
 */
class BourgetBackupAgent : BackupAgent() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        // Called on a backup thread, so blocking briefly on DataStore is fine. When backup is off
        // nothing is written, so the next backup holds no Bourget data.
        val enabled = runBlocking { TargetAppsStore(this@BourgetBackupAgent).backupEnabled.first() }
        if (enabled) super.onFullBackup(data)
    }

    /**
     * Consent is not carried to a new device: the user agrees again there before the service
     * reads anything. Rules, popup text and the intro state are kept.
     */
    override fun onRestoreFinished() {
        runBlocking { TargetAppsStore(this@BourgetBackupAgent).clearConsent() }
    }

    // Auto Backup (fullBackupOnly) only; the key/value callbacks are never used.
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) = Unit

    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit
}
