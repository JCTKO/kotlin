// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.impl.libraries.LibraryTableImplUtil
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.testFramework.rules.ProjectModelRule
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

@Suppress("UsePropertyAccessSyntax")
class ModuleLevelLibrariesInRootModelTest {
  companion object {
    @JvmField
    @ClassRule
    val appRule = ApplicationRule()
  }

  @Rule
  @JvmField
  val projectModel = ProjectModelRule()

  lateinit var module: Module

  @Before
  fun setUp() {
    module = projectModel.createModule()
  }

  @Test
  fun `add edit remove unnamed module library`() {
    run {
      val model = createModifiableModel(module)
      val library = model.moduleLibraryTable.createLibrary() as LibraryEx
      assertThat(model.moduleLibraryTable.libraries.single()).isEqualTo(library)
      val libraryEntry = dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.isModuleLevel).isTrue()
      assertThat(libraryEntry.libraryName).isNull()
      assertThat(libraryEntry.library).isEqualTo(library)
      assertThat(libraryEntry.scope).isEqualTo(DependencyScope.COMPILE)
      assertThat(libraryEntry.isExported).isFalse()
      assertThat(libraryEntry.libraryLevel).isEqualTo(LibraryTableImplUtil.MODULE_LEVEL)
      assertThat(model.findLibraryOrderEntry(library)).isEqualTo(libraryEntry)
      assertThat(library.isDisposed).isFalse()
      assertThat(library.module).isEqualTo(module)

      val committed = commitModifiableRootModel(model)
      val committedEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(committedEntry.scope).isEqualTo(DependencyScope.COMPILE)
      assertThat(committedEntry.isExported).isFalse()
      assertThat(committedEntry.isModuleLevel).isTrue()
      assertThat(committedEntry.libraryName).isNull()
      assertThat(committedEntry.library).isEqualTo(library)
      assertThat(committedEntry.libraryLevel).isEqualTo(LibraryTableImplUtil.MODULE_LEVEL)
      assertThat(library.isDisposed).isTrue()
      assertThat((committedEntry.library as LibraryEx).module).isEqualTo(module)
      assertThat((committedEntry.library as LibraryEx).isDisposed).isFalse()
    }

    val root = projectModel.baseProjectDir.newVirtualDirectory("lib")
    run {
      val model = createModifiableModel(module)
      val library = model.moduleLibraryTable.libraries.single()
      val libraryModel = library.modifiableModel
      libraryModel.addRoot(root, OrderRootType.CLASSES)
      libraryModel.commit()
      val libraryEntry = dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.getFiles(OrderRootType.CLASSES).single()).isEqualTo(root)
      libraryEntry.scope = DependencyScope.RUNTIME
      libraryEntry.isExported = true
      assertThat(model.findLibraryOrderEntry(library)).isEqualTo(libraryEntry)
      val committed = commitModifiableRootModel(model)
      val committedEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(committedEntry.getFiles(OrderRootType.CLASSES).single()).isEqualTo(root)
      assertThat((library as LibraryEx).isDisposed).isTrue()
      assertThat((committedEntry.library as LibraryEx).isDisposed).isFalse()
      assertThat(committedEntry.scope).isEqualTo(DependencyScope.RUNTIME)
      assertThat(committedEntry.isExported).isTrue()
    }

    run {
      val model = createModifiableModel(module)
      val library = model.moduleLibraryTable.libraries.single()
      model.moduleLibraryTable.removeLibrary(library)
      assertThat(model.moduleLibraryTable.libraries).isEmpty()
      assertThat(model.orderEntries).hasSize(1)
      assertThat(model.findLibraryOrderEntry(library)).isNull()
      val committed = commitModifiableRootModel(model)
      assertThat(committed.orderEntries).hasSize(1)
      assertThat((library as LibraryEx).isDisposed).isTrue()
    }
  }

  @Test
  fun `add rename remove module library`() {
    run {
      val model = createModifiableModel(module)
      val library = model.moduleLibraryTable.createLibrary("foo")
      assertThat(model.moduleLibraryTable.libraries.single()).isEqualTo(library)
      val libraryEntry = dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.libraryName).isEqualTo("foo")
      assertThat(libraryEntry.library).isEqualTo(library)

      val committed = commitModifiableRootModel(model)
      val committedEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(committedEntry.libraryName).isEqualTo("foo")
      assertThat(committedEntry.library).isEqualTo(library)
    }

    run {
      val model = createModifiableModel(module)
      val library = model.moduleLibraryTable.libraries.single()
      val libraryModel = library.modifiableModel
      libraryModel.name = "bar"
      libraryModel.commit()
      val libraryEntry = dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.libraryName).isEqualTo("bar")
      val committed = commitModifiableRootModel(model)
      val committedEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(committedEntry.libraryName).isEqualTo("bar")
    }

    run {
      val model = createModifiableModel(module)
      val libraryEntry = dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry
      model.removeOrderEntry(libraryEntry)
      assertThat(model.moduleLibraryTable.libraries).isEmpty()
      assertThat(model.orderEntries).hasSize(1)
      val committed = commitModifiableRootModel(model)
      assertThat(committed.orderEntries).hasSize(1)
    }
  }

  @Test
  fun `add module library and dispose`() {
    val model = createModifiableModel(module)
    val library = model.moduleLibraryTable.createLibrary("foo")
    val libraryEntry = dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry
    assertThat(libraryEntry.library).isEqualTo(library)
    model.dispose()

    dropModuleSourceEntry(ModuleRootManager.getInstance(module), 0)
    assertThat((library as LibraryEx).isDisposed).isTrue()
  }

  @Test
  fun `rename library before committing root model`() {
    val model = createModifiableModel(module)
    val library = model.moduleLibraryTable.createLibrary("foo")
    val libraryModel = library.modifiableModel
    assertThat((dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry).libraryName).isEqualTo("foo")
    libraryModel.name = "bar"
    assertThat((dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry).libraryName).isEqualTo("foo")
    libraryModel.commit()
    assertThat((dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry).libraryName).isEqualTo("bar")
    val committed = commitModifiableRootModel(model)
    assertThat((dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry).libraryName).isEqualTo("bar")
  }

  @Test
  fun `discard changes in library on disposing its modifiable model`() {
    val model = createModifiableModel(module)
    val library = model.moduleLibraryTable.createLibrary("foo")
    val libraryModel = library.modifiableModel
    val classesRoot = projectModel.baseProjectDir.newVirtualDirectory("classes")
    libraryModel.addRoot(classesRoot, OrderRootType.CLASSES)
    assertThat(libraryModel.getFiles(OrderRootType.CLASSES)).containsExactly(classesRoot)
    assertThat((dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry).getRootFiles(OrderRootType.CLASSES)).isEmpty()
    val committed = commitModifiableRootModel(model)
    assertThat((dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry).getRootFiles(OrderRootType.CLASSES)).isEmpty()
  }

  @Test
  fun `discard changes in library on disposing modifiable root model`() {
    run {
      val model = createModifiableModel(module)
      model.moduleLibraryTable.createLibrary("foo")
      commitModifiableRootModel(model)
    }
    val model = createModifiableModel(module)
    val library = model.moduleLibraryTable.libraries.single()
    val libraryModel = library.modifiableModel
    val classesRoot = projectModel.baseProjectDir.newVirtualDirectory("classes")
    libraryModel.addRoot(classesRoot, OrderRootType.CLASSES)
    assertThat(libraryModel.getFiles(OrderRootType.CLASSES)).containsExactly(classesRoot)
    libraryModel.commit()
    assertThat((dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry).getRootFiles(OrderRootType.CLASSES)).containsExactly(classesRoot)
    model.dispose()
    assertThat((dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry).getRootFiles(OrderRootType.CLASSES)).isEmpty()
  }

  @Test
  fun `two libraries with the same name`() {
    val model = createModifiableModel(module)
    val lib1 = model.moduleLibraryTable.createLibrary("foo")
    val lib2 = model.moduleLibraryTable.createLibrary("foo")
    val (entry1, entry2) = dropModuleSourceEntry(model, 2)
    assertThat((entry1 as LibraryOrderEntry).library).isEqualTo(lib1)
    assertThat((entry2 as LibraryOrderEntry).library).isEqualTo(lib2)
    val committed = commitModifiableRootModel(model)
    val (committedEntry1, committedEntry2) = dropModuleSourceEntry(committed, 2)
    assertThat((committedEntry1 as LibraryOrderEntry).library).isEqualTo(lib1)
    assertThat((committedEntry2 as LibraryOrderEntry).library).isEqualTo(lib2)
  }

  @Test
  fun `two unnamed libraries`() {
    val model = createModifiableModel(module)
    val lib1 = model.moduleLibraryTable.createLibrary()
    val lib2 = model.moduleLibraryTable.createLibrary()
    val (entry1, entry2) = dropModuleSourceEntry(model, 2)
    assertThat((entry1 as LibraryOrderEntry).library).isEqualTo(lib1)
    assertThat((entry2 as LibraryOrderEntry).library).isEqualTo(lib2)
    val committed = commitModifiableRootModel(model)
    val (committedEntry1, committedEntry2) = dropModuleSourceEntry(committed, 2)
    assertThat((committedEntry1 as LibraryOrderEntry).library).isEqualTo(lib1)
    assertThat((committedEntry2 as LibraryOrderEntry).library).isEqualTo(lib2)
  }
}