package io.aegis.core.context;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication context for Mutual TLS (mTLS) certificate authentication.
 *
 * @param certificate the client X.509 certificate
 * @param certificateChain the certificate chain
 * @param source the source of the certificate
 * @param attributes additional context attributes
 * @since 1.0.0
 */
public record MtlsContext(
        X509Certificate certificate,
        X509Certificate[] certificateChain,
        String source,
        Map<String, Object> attributes
) implements AuthenticationContext {

    /**
     * Creates an mTLS context with a certificate.
     *
     * @param certificate the client certificate
     */
    public MtlsContext(X509Certificate certificate) {
        this(certificate, new X509Certificate[]{certificate}, "tls", new HashMap<>());
    }

    /**
     * Creates an mTLS context with a certificate chain.
     *
     * @param certificateChain the certificate chain
     */
    public MtlsContext(X509Certificate[] certificateChain) {
        this(
                certificateChain != null && certificateChain.length > 0 ? certificateChain[0] : null,
                certificateChain,
                "tls",
                new HashMap<>()
        );
    }

    @Override
    public AuthenticationContextType getType() {
        return AuthenticationContextType.MTLS;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getSource() {
        return source;
    }

    /**
     * Gets the subject DN (Distinguished Name) from the certificate.
     *
     * @return the subject DN or null if certificate is null
     */
    public String getSubjectDN() {
        return certificate != null ? certificate.getSubjectX500Principal().getName() : null;
    }

    /**
     * Gets the issuer DN from the certificate.
     *
     * @return the issuer DN or null if certificate is null
     */
    public String getIssuerDN() {
        return certificate != null ? certificate.getIssuerX500Principal().getName() : null;
    }
}
