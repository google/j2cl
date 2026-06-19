/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.backend.kotlin

import com.google.j2cl.transpiler.ast.MemberDescriptor
import com.google.j2cl.transpiler.ast.TypeDeclaration

/** Set containing packages for which J2KT should translate actual visibility. */
private val ACTUAL_VISIBILITY_PACKAGES: Set<String> =
  setOf(
    "j2kt",
    "j2ktiosinterop",
    "j2ktjvminterop",
    "j2ktnotpassing",
    "j2ktnotpassing.nullmarked",
    "j2ktobjcweak",
    // copybara:strip_begin
    // Packages which should be hidden in Open Source project.
    "com.google.apps.xplat.collect.intervaltree",
    // copybara:strip_end
  )

/** List containing package prefixes for which J2KT should translate actual visibility. */
// Keep this list small as lookup time is linear.
private val ACTUAL_VISIBILITY_PACKAGE_PREFIXES: List<String> =
  listOf(
    "java.",
    "javaemul.",
    "javax.",
    // copybara:strip_begin
    // Packages which should be hidden in Open Source project.
    "com.google.apps.dynamite.v1.shared.",
    // copybara:strip_end
  )

/**
 * Set containing packages for which J2KT should not translate actual visibility. This list is used
 * to override the ACTUAL_VISIBILITY_PACKAGE_PREFIXES list.
 */
private val EXCLUDED_VISIBILITY_PACKAGES: Set<String> =
  setOf(
    // copybara:strip_begin
    // Packages which should be hidden in Open Source project.
    "com.google.apps.dynamite.v1.allshared.capabilities.group",
    "com.google.apps.dynamite.v1.allshared.converters",
    "com.google.apps.dynamite.v1.allshared.parser",
    "com.google.apps.dynamite.v1.allshared.util.emojisearch.util",
    "com.google.apps.dynamite.v1.shared.actions.api",
    "com.google.apps.dynamite.v1.shared.api.subscriptions",
    "com.google.apps.dynamite.v1.shared.api.subscriptions.snapshots.viewmodels",
    "com.google.apps.dynamite.v1.shared.capabilities.api",
    "com.google.apps.dynamite.v1.shared.common",
    "com.google.apps.dynamite.v1.shared.common.spacepermissions",
    "com.google.apps.dynamite.v1.shared.cse",
    "com.google.apps.dynamite.v1.shared.datamodels",
    "com.google.apps.dynamite.v1.shared.datamodels.converters",
    "com.google.apps.dynamite.v1.shared.events",
    "com.google.apps.dynamite.v1.shared.everythingelse",
    "com.google.apps.dynamite.v1.shared.mixins.api",
    "com.google.apps.dynamite.v1.shared.models.common.usersettings.genai",
    "com.google.apps.dynamite.v1.shared.multiplat.common.agentdisclaimer.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.agentfeedback.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.appstate.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.api.constants",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.banners.chatinmeetbanner.dismiss.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.banners.externalgdm.dismiss.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.banners.guestpassdmbanner.resendinvite.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.cancelactivity.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.config.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.finishsendscheduledmessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.integrationmenu.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.savedraft.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.sendbuttonoptions.showeditscheduledmessageoptions.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.sendbuttonoptions.showsendmessageoptions.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.sendscheduledmessagenow.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.updatecomposecontentstate.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.composebox.updatescheduledmessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.copytext.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.cse.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.genai.abusereport.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.legacyshortcut.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.legacyshortcut.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.common.messagescheduling.customtimeselection.schedulemessagewithtimevalidation.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.messagescheduling.customtimeselection.setscheduletime.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.messagescheduling.reschedulemessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.messagescheduling.schedulemessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.messagescheduling.schedulemessageeffect.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.messagescheduling.schedulingmenu.showcustomtimeselection.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.openurl.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.quotedmessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.recommendation.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.sessions.navigation.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.shortcutnavitem.navigatetogemini.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.shortcutnavitem.navigatetounsentmessageshortcut.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.shortcutnavitem.newbadgepromo.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.uigraph.api",
    "com.google.apps.dynamite.v1.shared.multiplat.common.unsentmessage.api.model",
    "com.google.apps.dynamite.v1.shared.multiplat.common.viewstate.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.chattab.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.composebar.smartcompose.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.home.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.home.filters.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.home.homeitem.snippet.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.home.homesummary.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.home.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.home.pills.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.home.promos.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.activitystate.thinking.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.activitystate.toggleexpanded.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.addtotasks.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.appcommand.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.appcommand.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.cancelpendingmessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.copymessagetext.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.deletemessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.editmessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.forwardmessage.api.models",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.forwardtoinbox.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.geministreamsummary.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.impl.uimodelproviders",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.markasunread.api.models",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.meetingrecap.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.meetingrecap.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.messagelabel.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.messagelist.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.messagereaction.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.messagetopicdata.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.notifyrendercompleted.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.pinmessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.quoteinreply.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.readreceipts.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.replyinthread.api.models",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.reportgenaiabuse.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.reportmessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.resend.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.saveallmedia.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.smartreply.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.spacesummary.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.spacetask.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.summarizefile.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.topicmutestate.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.typingstate.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.messagestream.unreaddivider.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.reactiondrawer.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.reactiondrawer.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.sessions.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.sessions.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.settings.autotranslate.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.settings.chatsummarization.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.settings.chattab.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.settings.devicenotifications.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.settings.navigatenotificationsetting.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.settings.navigation.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.settings.reactionnotifications.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.settings.smartcompose.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.settings.smartreplies.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.settings.themesetting.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.spacesettings.cancelspacepermissions.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.spacesettings.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.spacesettings.permissions.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.spacesettings.savespacepermissions.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.startchat.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.startchat.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.unsentmessageshortcut.deleteunsentmessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.unsentmessageshortcut.editunsentmessage.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.unsentmessageshortcut.forcereload.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.unsentmessageshortcut.sendnow.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.unsentmessageshortcut.showreschedulemenu.api",
    "com.google.apps.dynamite.v1.shared.multiplat.feature.unsentmessageshortcut.unsentmessagelist.api",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.analytics.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.control.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.cse.api",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.dialog.api",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.effects.api",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.executors.impl",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.heartbeat",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.j2kt",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.logging.api",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.network.punctual.streams",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.performance.api",
    "com.google.apps.dynamite.v1.shared.multiplat.infra.verbs.api",
    "com.google.apps.dynamite.v1.shared.network.api",
    "com.google.apps.dynamite.v1.shared.providers.uimemberdata",
    "com.google.apps.dynamite.v1.shared.providers.uimemberdata.api",
    "com.google.apps.dynamite.v1.shared.storage.api",
    "com.google.apps.dynamite.v1.shared.storage.controllers",
    "com.google.apps.dynamite.v1.shared.storage.controllers.api",
    "com.google.apps.dynamite.v1.shared.storage.controllers.refresh",
    "com.google.apps.dynamite.v1.shared.storage.coordinators",
    "com.google.apps.dynamite.v1.shared.storage.processors",
    "com.google.apps.dynamite.v1.shared.storage.processors.usergroupevents",
    "com.google.apps.dynamite.v1.shared.storage.schema",
    "com.google.apps.dynamite.v1.shared.subscriptions",
    "com.google.apps.dynamite.v1.shared.sync",
    "com.google.apps.dynamite.v1.shared.sync.api",
    "com.google.apps.dynamite.v1.shared.sync.blockedmessages.api",
    "com.google.apps.dynamite.v1.shared.sync.common",
    "com.google.apps.dynamite.v1.shared.sync.internal",
    "com.google.apps.dynamite.v1.shared.sync.prefetch",
    "com.google.apps.dynamite.v1.shared.sync.prefetch.api",
    "com.google.apps.dynamite.v1.shared.sync.state.api",
    "com.google.apps.dynamite.v1.shared.syncv2",
    "com.google.apps.dynamite.v1.shared.syncv2.api",
    "com.google.apps.dynamite.v1.shared.syncv2.coordinators",
    "com.google.apps.dynamite.v1.shared.syncv2.entities",
    "com.google.apps.dynamite.v1.shared.syncv2.subscriptions",
    "com.google.apps.dynamite.v1.shared.syncv2.subscriptions.manager.api",
    "com.google.apps.dynamite.v1.shared.syncv2.subscriptions.storeless",
    "com.google.apps.dynamite.v1.shared.testing.data.set",
    "com.google.apps.dynamite.v1.shared.testing.util.ioshelper",
    "com.google.apps.dynamite.v1.shared.uimodels",
    "com.google.apps.dynamite.v1.shared.uimodels.converters.api",
    "com.google.apps.dynamite.v1.shared.uimodels.impl",
    "com.google.apps.dynamite.v1.shared.users",
    "com.google.apps.dynamite.v1.shared.users.api",
    "com.google.apps.dynamite.v1.shared.util",
    "com.google.apps.dynamite.v1.shared.util.accountuser.api",
    "com.google.apps.dynamite.v1.shared.util.i18n.api",
    "com.google.apps.dynamite.v1.shared.util.tasks.steadyintervalthrottler",
    "com.google.apps.dynamite.v1.shared.util.url",
    "com.google.apps.dynamite.v1.shared.web.stream.shim",
    "com.google.apps.dynamite.v1.shared.web.world.manager",
    // copybara:strip_end
  )

private val TypeDeclaration.useActualKtVisibilityForPackage: Boolean
  get() = packageName.let { packageName ->
    packageName in ACTUAL_VISIBILITY_PACKAGES ||
      packageName.plus(".").let { packageNamePlusDot ->
        ACTUAL_VISIBILITY_PACKAGE_PREFIXES.any { packageNamePlusDot.startsWith(it) } &&
          packageName !in EXCLUDED_VISIBILITY_PACKAGES
      }
  }

/** Returns whether J2KT should translate actual visibility for the given type declaration. */
// TODO(b/206898384): Remove this once the bug is fixed.
internal val TypeDeclaration.useActualKtVisibility: Boolean
  get() =
    useActualKtVisibilityForPackage &&
      (!isAutoConverter || hasAutoValueOrBuilderSuperType) &&
      !isVisibilityWarningSuppressed

internal val MemberDescriptor.useActualKtVisibility: Boolean
  get() =
    enclosingTypeDescriptor.typeDeclaration.useActualKtVisibility && !isVisibilityWarningSuppressed
