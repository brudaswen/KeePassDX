/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.app.database.IOActionTask
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpElement
import java.util.*


class EntryViewModel: ViewModel() {

    val template : LiveData<Template> get() = _template
    private val _template = MutableLiveData<Template>()

    val mainEntryId : LiveData<NodeId<UUID>?> get() = _mainEntryId
    private val _mainEntryId = MutableLiveData<NodeId<UUID>?>()

    val historyPosition : LiveData<Int> get() = _historyPosition
    private val _historyPosition = MutableLiveData<Int>()

    val entryInfo : LiveData<EntryInfo> get() = _entryInfo
    private val _entryInfo = MutableLiveData<EntryInfo>()

    val entryHistory : LiveData<List<EntryInfo>> get() = _entryHistory
    private val _entryHistory = MutableLiveData<List<EntryInfo>>()

    val onOtpElementUpdated : LiveData<OtpElement?> get() = _onOtpElementUpdated
    private val _onOtpElementUpdated = SingleLiveEvent<OtpElement?>()

    val attachmentSelected : LiveData<Attachment> get() = _attachmentSelected
    private val _attachmentSelected = SingleLiveEvent<Attachment>()
    val onAttachmentAction : LiveData<EntryAttachmentState?> get() = _onAttachmentAction
    private val _onAttachmentAction = MutableLiveData<EntryAttachmentState?>()

    val historySelected : LiveData<EntryHistory> get() = _historySelected
    private val _historySelected = SingleLiveEvent<EntryHistory>()

    fun loadEntry(database: Database?, entryId: NodeId<UUID>?, historyPosition: Int) {
        if (database != null && entryId != null) {
            IOActionTask(
                {
                    val mainEntry = database.getEntryById(entryId)
                    val currentEntry = if (historyPosition > -1) {
                        mainEntry?.getHistory()?.get(historyPosition)
                    } else {
                        mainEntry
                    }

                    val entryTemplate = currentEntry?.let {
                        database.getTemplate(it)
                    } ?: Template.STANDARD

                    // To simplify template field visibility
                    currentEntry?.let { entry ->
                        // Add mainEntry to check the parent and define the template state
                        database.decodeEntryWithTemplateConfiguration(entry, mainEntry).let {
                            // To update current modification time
                            it.touch(modified = false, touchParents = false)

                            // Build history info
                            val entryInfoHistory = it.getHistory().map { entryHistory ->
                                entryHistory.getEntryInfo(database)
                            }

                            EntryInfoHistory(
                                mainEntry!!.nodeId,
                                entryTemplate,
                                it.getEntryInfo(database),
                                entryInfoHistory
                            )
                        }
                    }
                },
                { entryInfoHistory ->
                    if (entryInfoHistory != null) {
                        _mainEntryId.value = entryInfoHistory.mainEntryId
                        _historyPosition.value = historyPosition
                        _template.value = entryInfoHistory.template
                        _entryInfo.value = entryInfoHistory.entryInfo
                        _entryHistory.value = entryInfoHistory.entryHistory
                    }
                }
            ).execute()
        }
    }

    fun updateEntry(database: Database?) {
        loadEntry(database, _mainEntryId.value, _historyPosition.value ?: -1)
    }

    fun onOtpElementUpdated(optElement: OtpElement?) {
        _onOtpElementUpdated.value = optElement
    }

    fun onAttachmentSelected(attachment: Attachment) {
        _attachmentSelected.value = attachment
    }

    fun onAttachmentAction(entryAttachmentState: EntryAttachmentState?) {
        _onAttachmentAction.value = entryAttachmentState
    }

    fun onHistorySelected(item: EntryInfo, position: Int) {
        _historySelected.value = EntryHistory(NodeIdUUID(item.id), null, item, position)
    }

    data class EntryInfoHistory(var mainEntryId: NodeId<UUID>,
                                val template: Template,
                                val entryInfo: EntryInfo,
                                val entryHistory: List<EntryInfo>)
    // Custom data class to manage entry to retrieve and define is it's an history item (!= -1)
    data class EntryHistory(var nodeId: NodeId<UUID>,
                            var template: Template?,
                            var entryInfo: EntryInfo,
                            var historyPosition: Int = -1)

    companion object {
        private val TAG = EntryViewModel::class.java.name
    }
}