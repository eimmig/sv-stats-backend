package com.stakevault.betting.stats.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stakevault.betting.stats.domain.model.TenantSchemaName;
import com.stakevault.betting.stats.domain.port.in.AdminProvisionTenantUseCase;

@RestController
@RequestMapping("/api/v1/admin/tenants")
public class AdminTenantController {

	private final AdminProvisionTenantUseCase provisionTenant;

	public AdminTenantController(AdminProvisionTenantUseCase provisionTenant) {
		this.provisionTenant = provisionTenant;
	}

	@PostMapping
	public ResponseEntity<CreateTenantResponse> create(@RequestBody CreateTenantRequest request) {
		TenantSchemaName schema = provisionTenant.provisionTenant(request.slug());
		return ResponseEntity.status(HttpStatus.CREATED).body(new CreateTenantResponse(schema.value()));
	}

	@DeleteMapping("/{slug}")
	public ResponseEntity<Void> delete(@PathVariable String slug) {
		provisionTenant.deleteTenant(slug);
		return ResponseEntity.noContent().build();
	}
}
