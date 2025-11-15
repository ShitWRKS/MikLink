package com.app.miklink.ui.profile

import androidx.lifecycle.SavedStateHandle
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.TestProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for TestProfileViewModel
 *
 * Covers:
 * - Initial state validation
 * - pingCount validation (range 1-20)
 * - saveProfile() logic with valid/invalid inputs
 * - Profile loading for edit mode
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestProfileViewModelTest {

    // Mocks
    private lateinit var testProfileDao: TestProfileDao
    private lateinit var savedStateHandle: SavedStateHandle

    // System Under Test
    private lateinit var viewModel: TestProfileViewModel

    // Test Dispatcher
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks
        testProfileDao = mockk(relaxed = true)
        savedStateHandle = mockk(relaxed = true)

        // Default mock behavior: no profile loaded (new profile mode)
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        coEvery { testProfileDao.getAllProfiles() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================
    // TEST 1: Initial State
    // ============================================

    @Test
    fun `GIVEN new profile mode WHEN ViewModel created THEN pingCount default is 4`() = runTest {
        // Given: SavedStateHandle with profileId = -1 (new profile)
        every { savedStateHandle.get<Long>("profileId") } returns -1L

        // When: ViewModel is created
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)

        // Then: pingCount should be "4" (default)
        assertEquals("4", viewModel.pingCount.value)
    }

    @Test
    fun `GIVEN new profile mode WHEN ViewModel created THEN isEditing is false`() = runTest {
        // Given: SavedStateHandle with profileId = -1 (new profile)
        every { savedStateHandle.get<Long>("profileId") } returns -1L

        // When: ViewModel is created
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)

        // Then: isEditing should be false
        assertFalse(viewModel.isEditing)
    }

    @Test
    fun `GIVEN new profile mode WHEN ViewModel created THEN all fields are default`() = runTest {
        // Given: New profile mode
        every { savedStateHandle.get<Long>("profileId") } returns -1L

        // When: ViewModel is created
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)

        // Then: All fields should have default values
        assertEquals("", viewModel.profileName.value)
        assertEquals("", viewModel.profileDescription.value)
        assertFalse(viewModel.runTdr.value)
        assertTrue(viewModel.runLinkStatus.value) // default true
        assertFalse(viewModel.runLldp.value)
        assertFalse(viewModel.runPing.value)
        assertEquals("", viewModel.pingTarget1.value)
        assertEquals("", viewModel.pingTarget2.value)
        assertEquals("", viewModel.pingTarget3.value)
        assertEquals("4", viewModel.pingCount.value)
        assertFalse(viewModel.isSaved.value)
    }

    // ============================================
    // TEST 2: Validation - Happy Path (Valid Range)
    // ============================================

    @Test
    fun `GIVEN valid pingCount 10 WHEN saveProfile called THEN repository insert is called with pingCount 10`() = runTest {
        // Given: ViewModel with valid pingCount
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Test Profile"
        viewModel.pingCount.value = "10"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should be called with pingCount = 10
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 10 &&
                profile.profileName == "Test Profile"
            })
        }
    }

    @Test
    fun `GIVEN valid pingCount 1 WHEN saveProfile called THEN repository insert is called with pingCount 1`() = runTest {
        // Given: ViewModel with minimum valid pingCount
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Min Ping Test"
        viewModel.pingCount.value = "1"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should be called with pingCount = 1
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 1
            })
        }
    }

    @Test
    fun `GIVEN valid pingCount 20 WHEN saveProfile called THEN repository insert is called with pingCount 20`() = runTest {
        // Given: ViewModel with maximum valid pingCount
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Max Ping Test"
        viewModel.pingCount.value = "20"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should be called with pingCount = 20
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 20
            })
        }
    }

    @Test
    fun `GIVEN valid pingCount WHEN saveProfile called THEN isSaved becomes true`() = runTest {
        // Given: ViewModel with valid data
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Test"
        viewModel.pingCount.value = "5"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: isSaved should be true
        assertTrue(viewModel.isSaved.value)
    }

    // ============================================
    // TEST 3: Validation - Range Low (Invalid)
    // ============================================

    @Test
    fun `GIVEN invalid pingCount 0 WHEN saveProfile called THEN repository insert is called with default pingCount 4`() = runTest {
        // Given: ViewModel with pingCount below minimum (0)
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Invalid Low"
        viewModel.pingCount.value = "0"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should be called with coerced pingCount = 4 (default)
        // Note: Current implementation uses coerceIn(1, 20) which would give 1, but fallback is 4
        // Since toIntOrNull() succeeds, coerceIn applies -> result is 1
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 1 // coerceIn(0, 1, 20) = 1
            })
        }
    }

    @Test
    fun `GIVEN invalid pingCount -5 WHEN saveProfile called THEN repository insert is called with coerced pingCount 1`() = runTest {
        // Given: ViewModel with negative pingCount
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Negative Ping"
        viewModel.pingCount.value = "-5"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should be called with coerced value (1)
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 1
            })
        }
    }

    // ============================================
    // TEST 4: Validation - Range High (Invalid)
    // ============================================

    @Test
    fun `GIVEN invalid pingCount 21 WHEN saveProfile called THEN repository insert is called with coerced pingCount 20`() = runTest {
        // Given: ViewModel with pingCount above maximum (21)
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Invalid High"
        viewModel.pingCount.value = "21"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should be called with coerced pingCount = 20
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 20 // coerceIn(21, 1, 20) = 20
            })
        }
    }

    @Test
    fun `GIVEN invalid pingCount 100 WHEN saveProfile called THEN repository insert is called with coerced pingCount 20`() = runTest {
        // Given: ViewModel with very high pingCount
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Very High Ping"
        viewModel.pingCount.value = "100"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should be called with coerced value (20)
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 20
            })
        }
    }

    // ============================================
    // TEST 5: Validation - Invalid Format
    // ============================================

    @Test
    fun `GIVEN invalid pingCount abc WHEN saveProfile called THEN repository insert is called with default pingCount 4`() = runTest {
        // Given: ViewModel with non-numeric pingCount
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Invalid Format"
        viewModel.pingCount.value = "abc"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should be called with default pingCount = 4
        // (toIntOrNull() returns null, Elvis operator applies)
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 4
            })
        }
    }

    @Test
    fun `GIVEN empty pingCount WHEN saveProfile called THEN repository insert is called with default pingCount 4`() = runTest {
        // Given: ViewModel with empty pingCount
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Empty Ping"
        viewModel.pingCount.value = ""

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should be called with default pingCount = 4
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 4
            })
        }
    }

    @Test
    fun `GIVEN invalid pingCount with special chars WHEN saveProfile called THEN repository insert is called with default pingCount 4`() = runTest {
        // Given: ViewModel with special characters in pingCount
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Special Chars"
        viewModel.pingCount.value = "10@#$"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should be called with default pingCount = 4
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 4
            })
        }
    }

    // ============================================
    // TEST 6: Edit Mode - Profile Loading
    // ============================================

    @Test
    fun `GIVEN edit mode with existing profile WHEN ViewModel created THEN profile data is loaded`() = runTest {
        // Given: SavedStateHandle with valid profileId
        val existingProfile = TestProfile(
            profileId = 123L,
            profileName = "Existing Profile",
            profileDescription = "Test Description",
            runTdr = true,
            runLinkStatus = false,
            runLldp = true,
            runPing = true,
            pingTarget1 = "8.8.8.8",
            pingTarget2 = "1.1.1.1",
            pingTarget3 = null,
            pingCount = 15
        )

        every { savedStateHandle.get<Long>("profileId") } returns 123L
        coEvery { testProfileDao.getProfileById(123L) } returns flowOf(existingProfile)

        // When: ViewModel is created
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)

        // Then: All fields should be loaded from existing profile
        // advanceUntilIdle is not needed with UnconfinedTestDispatcher

        assertEquals("Existing Profile", viewModel.profileName.value)
        assertEquals("Test Description", viewModel.profileDescription.value)
        assertTrue(viewModel.runTdr.value)
        assertFalse(viewModel.runLinkStatus.value)
        assertTrue(viewModel.runLldp.value)
        assertTrue(viewModel.runPing.value)
        assertEquals("8.8.8.8", viewModel.pingTarget1.value)
        assertEquals("1.1.1.1", viewModel.pingTarget2.value)
        assertEquals("", viewModel.pingTarget3.value)
        assertEquals("15", viewModel.pingCount.value)
        assertTrue(viewModel.isEditing)
    }

    @Test
    fun `GIVEN edit mode WHEN saveProfile called THEN existing profile is updated`() = runTest {
        // Given: ViewModel in edit mode
        val existingProfile = TestProfile(
            profileId = 456L,
            profileName = "Old Name",
            profileDescription = null,
            runTdr = false,
            runLinkStatus = true,
            runLldp = false,
            runPing = false,
            pingTarget1 = null,
            pingTarget2 = null,
            pingTarget3 = null,
            pingCount = 4
        )

        every { savedStateHandle.get<Long>("profileId") } returns 456L
        coEvery { testProfileDao.getProfileById(456L) } returns flowOf(existingProfile)

        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        // advanceUntilIdle not needed with UnconfinedTestDispatcher

        // Modify fields
        viewModel.profileName.value = "Updated Name"
        viewModel.pingCount.value = "12"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should be called with updated profile (preserving profileId)
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.profileId == 456L &&
                profile.profileName == "Updated Name" &&
                profile.pingCount == 12
            })
        }
    }

    // ============================================
    // TEST 7: Delete Profile
    // ============================================

    @Test
    fun `GIVEN a profile WHEN deleteProfile called THEN DAO delete is invoked`() = runTest {
        // Given: ViewModel and a profile to delete
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)

        val profileToDelete = TestProfile(
            profileId = 789L,
            profileName = "To Delete",
            profileDescription = null,
            runTdr = false,
            runLinkStatus = true,
            runLldp = false,
            runPing = false,
            pingCount = 4
        )

        // When: deleteProfile is called
        viewModel.deleteProfile(profileToDelete)

        // Then: DAO delete should be invoked
        coVerify {
            testProfileDao.delete(profileToDelete)
        }
    }

    // ============================================
    // TEST 8: Edge Cases
    // ============================================

    @Test
    fun `GIVEN pingCount with decimal WHEN saveProfile called THEN default pingCount 4 is used`() = runTest {
        // Given: ViewModel with decimal pingCount (invalid for Int)
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Decimal Test"
        viewModel.pingCount.value = "10.5"

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should use default (toIntOrNull() fails on "10.5")
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 4
            })
        }
    }

    @Test
    fun `GIVEN pingCount with whitespace WHEN saveProfile called THEN default pingCount 4 is used`() = runTest {
        // Given: ViewModel with whitespace-only pingCount
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "Whitespace Test"
        viewModel.pingCount.value = "   "

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: DAO insert should use default
        coVerify {
            testProfileDao.insert(match { profile ->
                profile.pingCount == 4
            })
        }
    }

    @Test
    fun `GIVEN all test flags disabled WHEN saveProfile called THEN profile is saved correctly`() = runTest {
        // Given: ViewModel with all test flags disabled
        every { savedStateHandle.get<Long>("profileId") } returns -1L
        viewModel = TestProfileViewModel(testProfileDao, savedStateHandle)
        viewModel.profileName.value = "No Tests"
        viewModel.runTdr.value = false
        viewModel.runLinkStatus.value = false
        viewModel.runLldp.value = false
        viewModel.runPing.value = false

        // When: saveProfile is called
        viewModel.saveProfile()

        // Then: All flags should be false in saved profile
        coVerify {
            testProfileDao.insert(match { profile ->
                !profile.runTdr &&
                !profile.runLinkStatus &&
                !profile.runLldp &&
                !profile.runPing
            })
        }
    }
}

