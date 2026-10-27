package com.example.backend_sirhgp;

import com.example.backend_sirhgp.model.Employer;
import org.springframework.stereotype.Component;

/** Compatibility facade; the role chain is maintained in the service-layer resolver. */
@Deprecated
@Component("legacyPermissionApproverResolver")
public class PermissionApproverResolver {

    private final com.example.backend_sirhgp.service.PermissionApproverResolver delegate;

    public PermissionApproverResolver(com.example.backend_sirhgp.service.PermissionApproverResolver delegate) {
        this.delegate = delegate;
    }

    public Employer resolve(Employer requester) {
        return delegate.resolve(requester);
    }
}
