package co.fs.evo.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;

public class EvoClientDetails implements ClientDetails {
	
	private static final long serialVersionUID = -2139167369654154395L;
	
	private String clientId;
	private Set<String> resourceIds;
	private boolean secretRequired;
	private String clientSecret;
	private boolean isScoped;
	private Set<String> scopes;
	private Set<String> authorizedGrantTypes;
	private Set<String> registeredRedirectUri;
	private Collection<GrantedAuthority> authorities;
	private int accessTokenValiditySeconds;
	private int refreshTokenValiditySeconds;

	@Override
	public String getClientId() {
		return clientId;
	}
	
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@Override
	public Set<String> getResourceIds() {
		return resourceIds;
	}
	
	public void setResourceIds(ArrayList<String> resourceIds) {
		this.resourceIds = new HashSet<String>(resourceIds);
	}

	@Override
	public boolean isSecretRequired() {
		return secretRequired;
	}
	
	public void setIsSecretRequired(boolean secretRequired) {
		this.secretRequired = secretRequired;
	}

	@Override
	public String getClientSecret() {
		return clientSecret;
	}
	
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	@Override
	public boolean isScoped() {
		return isScoped;
	}
	
	public void setIsScoped(boolean isScoped) {
		this.isScoped = isScoped;
	}

	@Override
	public Set<String> getScope() {
		return scopes;
	}
	
	public void setScope(ArrayList<String> scopes) {
		this.scopes = new HashSet<String>(scopes);
	}

	@Override
	public Set<String> getAuthorizedGrantTypes() {
		return authorizedGrantTypes;
	}
	
	public void setAuthorizedGrantTypes(ArrayList<String> authorizedGrantTypes) {
		this.authorizedGrantTypes = new HashSet<String>(authorizedGrantTypes);
	}

	@Override
	public Set<String> getRegisteredRedirectUri() {
		return registeredRedirectUri;
	}
	
	public void setRegisteredRedirectUri(ArrayList<String> registeredRedirectUri) {
		this.registeredRedirectUri = new HashSet<String>(registeredRedirectUri);
	}

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		return authorities;
	}

	public void setAuthorities(ArrayList<String> authorities) {
		for (String authority: authorities) {
			this.authorities.add(new SimpleGrantedAuthority(authority));
		}
	}
	
	@Override
	public Integer getAccessTokenValiditySeconds() {
		return accessTokenValiditySeconds;
	}

	public void setAccessTokenValiditySeconds(int seconds) {
		this.accessTokenValiditySeconds = seconds;
	}
	
	@Override
	public Integer getRefreshTokenValiditySeconds() {
		return refreshTokenValiditySeconds;
	}
	
	public void setRefreshTokenValiditySeconds(int seconds) {
		this.refreshTokenValiditySeconds = seconds;
	}

	@Override
	public Map<String, Object> getAdditionalInformation() {
		return null;
	}

}
