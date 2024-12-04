package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.model.RemoteUser;
import com.bmg.deliver.repository.RemoteUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoteUserServiceImplTest {

	@InjectMocks
	private RemoteUserServiceImpl remoteUserService;

	@Mock
	private RemoteUserRepository remoteUserRepository;

	private RemoteUser remoteUser;
	private List<RemoteUser> remoteUserList;

	@BeforeEach
	void setUp() {
		remoteUser = new RemoteUser();
		remoteUser.setId(1L);
		remoteUser.setUsername("testuser");

		remoteUserList = new ArrayList<>();
		remoteUserList.add(remoteUser);
	}

	@Test
    void testAddUserSuccess() {
        when(remoteUserRepository.save(any(RemoteUser.class))).thenReturn(remoteUser);
        RemoteUser savedUser = remoteUserService.addUser(remoteUser);
        assertNotNull(savedUser);
        assertEquals("testuser", savedUser.getUsername());
        verify(remoteUserRepository, times(1)).save(remoteUser);
    }

	@Test
    void testAddUserException() {
        when(remoteUserRepository.save(any(RemoteUser.class))).thenThrow(new RuntimeException("Error saving user"));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> remoteUserService.addUser(remoteUser));
        assertEquals("Error saving user", exception.getMessage());
        verify(remoteUserRepository, times(1)).save(remoteUser);
    }

	@Test
    void testGetAllUsersSuccess() {
        when(remoteUserRepository.findAll()).thenReturn(remoteUserList);
        List<RemoteUser> users = remoteUserService.getAllUsers();
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("testuser", users.get(0).getUsername());
        verify(remoteUserRepository, times(1)).findAll();
    }

	@Test
    void testGetAllUsersEmpty() {
        when(remoteUserRepository.findAll()).thenReturn(new ArrayList<>());
        List<RemoteUser> users = remoteUserService.getAllUsers();
        assertNotNull(users);
        assertTrue(users.isEmpty());
        verify(remoteUserRepository, times(1)).findAll();
    }

	@Test
    void testGetUserByUsernameFound() {
        when(remoteUserRepository.findByUsername(anyString())).thenReturn(Optional.of(remoteUser));
        Optional<RemoteUser> foundUser = remoteUserService.getUserByUsername("testuser");
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
        verify(remoteUserRepository, times(1)).findByUsername(anyString());
    }

	@Test
    void testGetUserByUsernameNotFound() {
        when(remoteUserRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        Optional<RemoteUser> foundUser = remoteUserService.getUserByUsername("testuser");
        assertFalse(foundUser.isPresent());
        verify(remoteUserRepository, times(1)).findByUsername(anyString());
    }
}
