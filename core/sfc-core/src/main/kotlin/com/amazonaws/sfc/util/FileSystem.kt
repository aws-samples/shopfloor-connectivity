
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util

import com.amazonaws.sfc.system.isWindowsSystem
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.*

fun Path.ensureExists(): Path {
    if (!Files.exists(this)) {
        Files.createDirectories(this)
    }
    return this
}

fun checkFileConstraints(file: File): Boolean =
    if (!isWindowsSystem()) {
        checkFileConstraintsPosix(file)
    } else {
        checkFileConstraintsWindows(file)
    }

fun constrainFilePermissions(file: File) {
    if (isWindowsSystem()) {
        constrainFilePermissionWindows(file)
    } else {
        constrainFilePermissionsPosix(file)
    }
}


// Returns false if guest or Everyone has any permissions
private fun checkFileConstraintsWindows(file: File): Boolean {
    val aclView = Files.getFileAttributeView(file.toPath(), AclFileAttributeView::class.java)
    val acl = aclView.acl
    return acl.any { entry ->
        val hasAnyPermissions = entry.permissions().isNotEmpty()
        entry.type() == AclEntryType.ALLOW && isRestrictedUserWindows(entry) && hasAnyPermissions
    }
}

// Returns false if users other than the owner or group users have any permissions
private fun checkFileConstraintsPosix(file: File): Boolean {
    val permissions = Files.getAttribute(file.toPath(), "posix:permissions") as Set<*>
    return permissions.none { isPermissionForOtherUserPosix(it) }
}


private fun isPermissionForOtherUserPosix(it: Any?) = it is PosixFilePermission && listOf("OTHERS_READ", "OTHERS_WRITE", "OTHERS_EXECUTE").contains(it.name)


// chmod 660
private fun constrainFilePermissionsPosix(file: File) {
    val permissions = Files.getPosixFilePermissions(file.toPath())
    permissions.remove(PosixFilePermission.OTHERS_WRITE)
    permissions.remove(PosixFilePermission.OTHERS_READ)
    Files.setPosixFilePermissions(file.toPath(), permissions)
}


// Give full permission to owner of the file and remove permissions for guest and everyone
private fun constrainFilePermissionWindows(file: File) {

    val owner = Files.getOwner(file.toPath())
    val aclView = Files.getFileAttributeView(file.toPath(), AclFileAttributeView::class.java)
    val acl = aclView.acl

    val ownerAllow = AclEntry.newBuilder()
        .setType(AclEntryType.ALLOW)
        .setPrincipal(owner)
        .setPermissions(
            // Full permissions for owner
            AclEntryPermission.READ_DATA,
            AclEntryPermission.WRITE_DATA,
            AclEntryPermission.APPEND_DATA,
            AclEntryPermission.READ_NAMED_ATTRS,
            AclEntryPermission.WRITE_NAMED_ATTRS,
            AclEntryPermission.READ_ATTRIBUTES,
            AclEntryPermission.WRITE_ATTRIBUTES,
            AclEntryPermission.DELETE,
            AclEntryPermission.READ_ACL,
            AclEntryPermission.WRITE_ACL,
            AclEntryPermission.WRITE_OWNER,
            AclEntryPermission.SYNCHRONIZE
        )
        .build()

    val aceList = mutableListOf(ownerAllow)

    aceList.addAll(acl.filter { entry ->
        // owner already in the list
        val isNotTheOwner = entry.principal() != owner
        val permissionsForAllowedUsers = entry.type() == AclEntryType.ALLOW && !isRestrictedUserWindows(entry)
        (entry.type() == AclEntryType.DENY) || (permissionsForAllowedUsers && isNotTheOwner)
    })

    aclView.acl = aceList
}

fun currentDirectory(): String {
    return System.getProperty("user.dir")
}


private val windowsRestrictedUsersAndGroups = listOf("Guest", "Everyone")
private fun isRestrictedUserWindows(entry: AclEntry) = windowsRestrictedUsersAndGroups.firstOrNull { entry.principal().name.endsWith("\\$it") } != null