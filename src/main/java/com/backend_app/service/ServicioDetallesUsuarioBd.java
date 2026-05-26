package com.backend_app.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.backend_app.config.PrincipalUsuarioAutenticado;
import com.backend_app.model.Usuario;
import com.backend_app.repository.RepositorioUsuarios;

@Service
public class ServicioDetallesUsuarioBd implements UserDetailsService {
	private final RepositorioUsuarios repositorioUsuarios;

	public ServicioDetallesUsuarioBd(RepositorioUsuarios repositorioUsuarios) {
		this.repositorioUsuarios = repositorioUsuarios;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Usuario user = repositorioUsuarios.findByEmailIgnoreCase(username)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

		return new PrincipalUsuarioAutenticado(user.getId(), user.getStore().getId(), user.getRole(), user.getEmail(),
				user.getPasswordHash(), user.isActive());
	}
}
