import { useState, useEffect } from 'react';
import './App.css';

// Embedded test private key in JWK format matching the public key configured in the backend
const TEST_PRIVATE_KEY_JWK = {
  "kty": "RSA",
  "n": "tzBhHDmFE0O1nv4B2ZWnVmuvswS9Vrzz2swgQ49smMgTdCi-q_8Ka-bH99AiQab78dM1vk0Q8ot2_Ha-mgBeG2G0CTFDMLySjVxGiBcFI-Z-4t5_nPlFVTsjz5Sjbt2piFCEZqZzEEybb6nbLCXdzokCAvURPBwCAwSr1iLzFTxiHphPUYvG4mdQEJgvJSVbECP9YvXLJwHzvARsh3hWVE2LxTsa2xIZa2cukqi05S0G4oQxbFJHlDwoh5FyzHctFnArzVDESHaEeSOeI_oUNLyZHwQxxD1SRuZ1XsYr-hziEciv4Le5aOWs7At4bHh6vz25HX__fpDMUkB8c7Xcdw",
  "e": "AQAB",
  "d": "FbtTieROTn7AfmARGsg6Kx0TTDV1ELknD1Rv72kwWjTsueGrh6lLNIm9kenLETwctLojAgm6ckuWjf9i6noTWpmN9hk1_gN2OfbcbICZR4JXQyq0u4vcFxmw9zXhkuFmSe7jW29w7wS829OsAIzCx3b8GhsLNF-jjXVxvTGK4istXnjPq7yoUeEAnsrlYUzuQDInOMmbDhW3DNcrK_RltaTgA8Ga3dQo0ev94V93bnqv7oTegHLcwI_5pdPPt-PR_0CGmsK8e9Ew6CVg23W4i6D8IJNrLP90PugORKjP5NNuRojSe8K5uCOkxClq4z16UHzP9StWazH0RSYBxhv9IQ",
  "p": "7qL1rioac5lsKwuo4DBCAzp8AG7TkMqGz4H8jDmE5B125PGmiluqYpnni12MzS0uPT6w8O3BlLydSp1YdvlLWzq8hgz7_7UnojjPK_if9zFN3RXclInSD_Eu2sUuXbAFkDG6gjdp3ZaqeLgqXhbkXPSg-VI724j0-j02SGepieU",
  "q": "xISbiNBIq0WDVoyGJ4ZlLguk2KL5I8Lhyz0H63DeRhyswdmhKodtd09BNq1yizHB0H43toHE52nWo5iO8Ha53133rikKSfURyl4nGtFw5iI_BQoXd0g0sLe-SE07SIzNapJrki4_UmW-y86cP_tNKVE0YF7vo-XhB6E8YFd1tys",
  "dp": "SjdILRhPDbCjYWfI56Bah2KC-id9iMRT1OlaP8oLuF4pgd5dqx4DCZNP3ZoEljL89HMw2F05HSbjzDbPMoEpnH_R7ebP4KDYaK0-UTCLn3cn_iA0b8XFHMwnhEZauyxpLoUouiK9u_qFnfG4y3ZXI0m5XpDiqM4ZUlIDNdV3drk",
  "dq": "eKFB7CCWivPXpDgMXZTE5Rfmr8iSkF4fRieHhgG5n2YYscHKiZWqH1O6HzsnFcSMSVRBFLnhyX-RbsjF7VujyzYeRH0SwMU7j3JuJKst10ZsUsaYEvNyzItttWobGvS7X1DT0V6sJgMotGh2R1wWSGd9dC6ygXQpxwo1SppFOxM",
  "qi": "x0h5WWp8TgBW1jY142PnpNQvquLhiznwx73kjtzfA3VqTg3e6bXlPuVInm2AGMvvhqRp7vab7rrq3ixWUWTEvpq4py0WFElLwVtmIrC9UpX0J8Q2YyvhJuZH1TZVWJYSDz3UTQRSKZxOaXENAD0qYSIL1AFraKYqRJi81vI8TUc"
};

// Types & Interfaces
interface FieldConstraint {
  min?: number;
  max?: number;
  positive?: boolean;
}

interface FieldMetadata {
  name: string;
  type: string;
  required: boolean;
  constraints: FieldConstraint;
}

interface ResourceInfo {
  basePath: string;
  version: string;
  entityClass: string;
  dtoClass: string;
  fields: FieldMetadata[];
}

interface MetadataResponse {
  [resourceKey: string]: ResourceInfo;
}

interface QueryFilter {
  field: string;
  operator: '_like' | '_gt' | '_lt' | '_gte' | '_lte' | '=';
  value: string;
}

// Fallback Mock data if Backend is not running
const MOCK_METADATA: MetadataResponse = {
  "products": {
    "basePath": "products",
    "version": "v1",
    "entityClass": "com.example.crudapp.data.Product",
    "dtoClass": "com.example.crudapp.api.records.ProductRecord",
    "fields": [
      { "name": "name", "type": "String", "required": true, "constraints": { "min": 2, "max": 100 } },
      { "name": "description", "type": "String", "required": false, "constraints": {} },
      { "name": "price", "type": "Double", "required": true, "constraints": { "positive": true } },
      { "name": "attributes", "type": "Map", "required": false, "constraints": {} }
    ]
  }
};

const ARCH_NODES = [
  {
    id: 'client',
    title: 'Client Webapp',
    subtitle: 'React-TS & Vite',
    desc: 'Communicates with REST endpoints by signing OAuth2-compliant JWT tokens natively in the browser via the Web Crypto API, supporting correlation request tracing (X-Request-ID) and multi-tenancy.'
  },
  {
    id: 'rate_limiter',
    title: 'Rate Limiter',
    subtitle: 'Token Bucket Filter',
    desc: 'Intercepts incoming WebFlux requests, enforcing a sliding Token Bucket limit of 50 requests/min per IP to block denial-of-service attempts. Returns standard RFC 7807 (HTTP 429) details when exceeded.'
  },
  {
    id: 'gateway',
    title: 'API Controllers',
    subtitle: 'Byte Buddy Beans',
    desc: 'At startup, a BeanFactoryPostProcessor scans annotations and uses Byte Buddy to dynamically compile and register separate WebFlux RestController classes for each entity at runtime.'
  },
  {
    id: 'exceptions',
    title: 'RFC 7807 Handler',
    subtitle: 'Unified ControllerAdvice',
    desc: 'A unified handler intercepts database integrity errors, validation violations, concurrency conflicts, and security/access exceptions, mapping them to standard RFC 7807 Problem Details.'
  },
  {
    id: 'security',
    title: 'Security Filters',
    subtitle: 'Reactive WebFilter',
    desc: 'A ReactiveJwtFilter validates the cryptographic token signature, extracts roles and tenant variables, validates authorization access rules, and propagates values into the Reactor context.'
  },
  {
    id: 'service',
    title: 'Decoupled Core',
    subtitle: 'Service Registry',
    desc: 'Delegates business logic. Decouples controllers from entities via a ServiceRegistry lookup that returns custom service overrides or defaults to a dynamic reactive CrudService.'
  },
  {
    id: 'data',
    title: 'Dynamic Data Access',
    subtitle: 'JPA Graphs & Specs',
    desc: 'Executes database queries on boundedElastic scheduler threads. Creates dynamic JPA specifications from query filters and injects Entity Graphs to solve the N+1 select problem.'
  },
  {
    id: 'database',
    title: 'RLS Database',
    subtitle: 'PostgreSQL RLS',
    desc: 'Enforces Row-Level Security (RLS) policies based on transaction-level context setting via SET LOCAL app.current_tenant, isolating records dynamically per tenant.'
  }
];

function App() {
  const [backendUrl, setBackendUrl] = useState('http://localhost:8080');

  // Detect deployed backend URL on mount
  useEffect(() => {
    const detectUrl = async () => {
      // 1. Check query parameter first for manual overrides
      const params = new URLSearchParams(window.location.search);
      const queryUrl = params.get('backendUrl');
      if (queryUrl) {
        console.log("Detected backendUrl from query parameter:", queryUrl);
        setBackendUrl(queryUrl);
        return;
      }

      // 2. Fetch deployed configuration
      try {
        const response = await fetch('./backend-url.json');
        if (response.ok) {
          const data = await response.json();
          if (data.url) {
            console.log("Detected backendUrl from backend-url.json:", data.url);
            setBackendUrl(data.url);
          }
        }
      } catch (e) {
        console.log("No deployed backend-url.json found, using default:", backendUrl);
      }
    };
    detectUrl();
  }, []);
  const [username, setUsername] = useState('admin-user');
  const [tenantId, setTenantId] = useState('tenant-a');
  const [selectedRole, setSelectedRole] = useState<'ADMIN' | 'GUEST' | 'NONE'>('ADMIN');
  const [jwtToken, setJwtToken] = useState('');
  
  const [metadata, setMetadata] = useState<MetadataResponse | null>(null);
  const [selectedResource, setSelectedResource] = useState('products');
  const [selectedOp, setSelectedOp] = useState<'GET_ALL' | 'GET_BY_ID' | 'POST' | 'PUT' | 'DELETE'>('GET_ALL');
  
  // Dynamic Tester Fields
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sortField, setSortField] = useState('id');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const [queryFilters, setQueryFilters] = useState<QueryFilter[]>([]);
  
  const [singleId, setSingleId] = useState('');
  const [formData, setFormData] = useState<Record<string, any>>({});
  
  // Console state
  const [consoleReq, setConsoleReq] = useState({ url: '', method: '', headers: {} as Record<string, string>, body: '' });
  const [consoleRes, setConsoleRes] = useState({ status: 0, statusText: '', headers: {} as Record<string, string>, body: '', isError: false, isPending: false });
  
  const [activeArchNode, setActiveArchNode] = useState(ARCH_NODES[0]);
  const [isUsingMock, setIsUsingMock] = useState(false);

  // Generate JWT token on the fly
  const generateToken = async () => {
    try {
      const header = { alg: "RS256", typ: "JWT", kid: "test-key-id" };
      const roles = selectedRole !== 'NONE' ? [selectedRole] : [];
      const payload = {
        sub: username,
        preferred_username: username,
        tenant: tenantId,
        realm_access: { roles },
        iss: "http://localhost:8081/realms/crud-realm",
        exp: Math.floor(Date.now() / 1000) + 3600
      };

      const base64UrlEncode = (arr: Uint8Array) => {
        return btoa(String.fromCharCode(...arr))
          .replace(/=/g, "")
          .replace(/\+/g, "-")
          .replace(/\//g, "_");
      };

      const textEncoder = new TextEncoder();
      const headerStr = base64UrlEncode(textEncoder.encode(JSON.stringify(header)));
      const payloadStr = base64UrlEncode(textEncoder.encode(JSON.stringify(payload)));
      const tokenInput = `${headerStr}.${payloadStr}`;

      const key = await window.crypto.subtle.importKey(
        "jwk",
        TEST_PRIVATE_KEY_JWK,
        {
          name: "RSASSA-PKCS1-v1_5",
          hash: { name: "SHA-256" }
        },
        false,
        ["sign"]
      );

      const signature = await window.crypto.subtle.sign(
        "RSASSA-PKCS1-v1_5",
        key,
        textEncoder.encode(tokenInput)
      );

      const signatureStr = base64UrlEncode(new Uint8Array(signature));
      const completeToken = `${tokenInput}.${signatureStr}`;
      setJwtToken(completeToken);
    } catch (e) {
      console.error("Token generation failed", e);
    }
  };

  // Load API Metadata from Spring Boot
  const fetchMetadata = async () => {
    try {
      const response = await fetch(`${backendUrl}/api/metadata`, {
        headers: {
          'Bypass-Tunnel-Reminder': 'true'
        }
      });
      if (response.ok) {
        const data = await response.json();
        setMetadata(data);
        setIsUsingMock(false);
        const keys = Object.keys(data);
        if (keys.length > 0) {
          setSelectedResource(keys[0]);
        }
      } else {
        throw new Error("Metadata request failed");
      }
    } catch (e) {
      console.warn("Backend metadata could not be fetched. Using mock fallback.", e);
      setMetadata(MOCK_METADATA);
      setIsUsingMock(true);
      setSelectedResource('products');
    }
  };

  useEffect(() => {
    fetchMetadata();
  }, [backendUrl]);

  useEffect(() => {
    generateToken();
  }, [username, tenantId, selectedRole]);

  // Set form defaults when selected resource metadata loads
  useEffect(() => {
    if (metadata && metadata[selectedResource]) {
      const defaults: Record<string, any> = {};
      metadata[selectedResource].fields.forEach(f => {
        if (f.type === 'Double' || f.type === 'Float' || f.type === 'Integer' || f.type === 'Long') {
          defaults[f.name] = f.constraints.positive ? 1.0 : 0.0;
        } else if (f.type === 'Boolean') {
          defaults[f.name] = false;
        } else if (f.type === 'Map') {
          defaults[f.name] = { color: 'blue', size: 'large' };
        } else {
          defaults[f.name] = '';
        }
      });
      setFormData(defaults);
    }
  }, [selectedResource, metadata]);

  // Execute API Request
  const executeRequest = async () => {
    if (!metadata || !metadata[selectedResource]) return;
    const resInfo = metadata[selectedResource];
    
    // Client-side validation for ID-based operations
    const needsId = (selectedOp === 'GET_BY_ID' || selectedOp === 'PUT' || selectedOp === 'DELETE');
    if (needsId) {
      if (!singleId || !/^\d+$/.test(singleId.trim())) {
        setConsoleReq({ 
          url: `${backendUrl}/api/${resInfo.version}/${selectedResource}/${singleId || '{id}'}`, 
          method: selectedOp === 'GET_BY_ID' ? 'GET' : selectedOp, 
          headers: { 'Authorization': 'Bearer ...' }, 
          body: '' 
        });
        setConsoleRes({
          status: 400,
          statusText: 'Client Validation Error',
          headers: {},
          body: `Client Validation Error: ID must be a positive integer.\nReceived ID: "${singleId}"`,
          isError: true,
          isPending: false
        });
        return;
      }
    }

    let path = `${backendUrl}/api/${resInfo.version}/${selectedResource}`;
    let method = 'GET';
    let headers: Record<string, string> = {
      'Accept': 'application/json',
      'Authorization': `Bearer ${jwtToken}`,
      'Bypass-Tunnel-Reminder': 'true'
    };
    let body = '';

    if (selectedOp === 'GET_ALL') {
      const params = new URLSearchParams();
      params.append('page', String(page));
      params.append('size', String(size));
      if (sortField) {
        params.append('sort', `${sortField},${sortOrder}`);
      }
      queryFilters.forEach(f => {
        if (f.field && f.value) {
          const key = f.operator === '=' ? f.field : `${f.field}${f.operator}`;
          params.append(key, f.value);
        }
      });
      path += `?${params.toString()}`;
    } else if (selectedOp === 'GET_BY_ID') {
      path += `/${singleId}`;
    } else if (selectedOp === 'POST') {
      method = 'POST';
      headers['Content-Type'] = 'application/json';
      body = JSON.stringify(formData, null, 2);
    } else if (selectedOp === 'PUT') {
      method = 'PUT';
      headers['Content-Type'] = 'application/json';
      path += `/${singleId}`;
      body = JSON.stringify(formData, null, 2);
    } else if (selectedOp === 'DELETE') {
      method = 'DELETE';
      path += `/${singleId}`;
    }

    setConsoleReq({ url: path, method, headers, body });
    setConsoleRes(prev => ({ ...prev, isPending: true, isError: false }));

    try {
      const response = await fetch(path, {
        method,
        headers,
        body: method !== 'GET' && method !== 'DELETE' ? body : undefined
      });

      const resHeaders: Record<string, string> = {};
      response.headers.forEach((v, k) => {
        resHeaders[k] = v;
      });

      let resText = await response.text();
      let parsedBody = resText;
      try {
        parsedBody = JSON.stringify(JSON.parse(resText), null, 2);
      } catch {
        // Not JSON
      }

      setConsoleRes({
        status: response.status,
        statusText: response.statusText,
        headers: resHeaders,
        body: parsedBody,
        isError: !response.ok,
        isPending: false
      });
    } catch (err: any) {
      setConsoleRes({
        status: 500,
        statusText: 'Fetch Error',
        headers: {},
        body: err.message || 'Failed to connect to the backend server.',
        isError: true,
        isPending: false
      });
    }
  };

  const addFilter = () => {
    if (!metadata || !metadata[selectedResource]) return;
    const fields = metadata[selectedResource].fields;
    if (fields.length > 0) {
      setQueryFilters([...queryFilters, { field: fields[0].name, operator: '=', value: '' }]);
    }
  };

  const removeFilter = (index: number) => {
    const updated = [...queryFilters];
    updated.splice(index, 1);
    setQueryFilters(updated);
  };

  const handleFilterChange = (index: number, key: keyof QueryFilter, value: string) => {
    const updated = [...queryFilters];
    updated[index] = { ...updated[index], [key]: value };
    setQueryFilters(updated);
  };

  const handleFormChange = (fieldName: string, value: any, type: string) => {
    let parsedVal = value;
    if (type === 'Double' || type === 'Float' || type === 'Integer' || type === 'Long') {
      parsedVal = parseFloat(value);
      if (isNaN(parsedVal)) parsedVal = value;
    } else if (type === 'Boolean') {
      parsedVal = value === 'true';
    } else if (type === 'Map') {
      try {
        parsedVal = JSON.parse(value);
      } catch {
        parsedVal = value; // Keep string editing until fully valid json is entered
      }
    }
    setFormData({ ...formData, [fieldName]: parsedVal });
  };

  const currentResource = metadata ? metadata[selectedResource] : null;

  return (
    <div className="dashboard">
      
      {/* 1. Glassmorphic Header */}
      <header className="header glass-panel">
        <div className="title-area">
          <h1>Dynamic CRUD Engine Platform</h1>
          <p>Metadata-Driven Reactive Micro-Service Panel</p>
        </div>
        <div className="badge-version">WEBFLUX v4.0.6</div>
      </header>

      {/* 2. Architectural Diagram Block */}
      <section className="arch-container glass-panel">
        <div className="arch-header">
          <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 600 }}>System Architecture Blueprint</h3>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Click on any component to inspect details</span>
        </div>
        <div className="arch-flow">
          {ARCH_NODES.map((node, i) => (
            <div key={node.id} style={{ display: 'contents' }}>
              <div 
                className={`arch-node ${activeArchNode.id === node.id ? 'active' : ''}`}
                onClick={() => setActiveArchNode(node)}
              >
                <h4>{node.title}</h4>
                <span>{node.subtitle}</span>
              </div>
              {i < ARCH_NODES.length - 1 && (
                <div className="arch-arrow">➜</div>
              )}
            </div>
          ))}
        </div>
        <div className="arch-detail-panel">
          <h5>⚡ {activeArchNode.title} ({activeArchNode.subtitle})</h5>
          <p>{activeArchNode.desc}</p>
        </div>
      </section>

      {/* 3. Columns Layout */}
      <div className="main-grid">
        
        {/* Left Column: Security Control Panel */}
        <aside className="sidebar-panel glass-panel">
          <div>
            <h3>Security & Tenancy</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', margin: '4px 0 0 0' }}>Configure credentials to test endpoint policies</p>
          </div>
          
          <div className="form-group">
            <label>Tenant ID</label>
            <input 
              type="text" 
              className="input-control" 
              value={tenantId} 
              onChange={e => setTenantId(e.target.value)} 
            />
          </div>

          <div className="form-group">
            <label>Preferred Username</label>
            <input 
              type="text" 
              className="input-control" 
              value={username} 
              onChange={e => setUsername(e.target.value)} 
            />
          </div>

          <div className="form-group">
            <label>Identity Role Mapping</label>
            <div style={{ display: 'flex', gap: '8px' }}>
              {(['ADMIN', 'GUEST', 'NONE'] as const).map(role => (
                <button
                  key={role}
                  className={`btn-secondary`}
                  style={{ 
                    flex: 1, 
                    justifyContent: 'center',
                    borderColor: selectedRole === role ? 'var(--accent-cyan)' : 'var(--panel-border)',
                    color: selectedRole === role ? 'var(--accent-cyan)' : 'var(--text-secondary)',
                    background: selectedRole === role ? 'rgba(6, 182, 212, 0.08)' : 'rgba(255, 255, 255, 0.02)'
                  }}
                  onClick={() => setSelectedRole(role)}
                >
                  {role}
                </button>
              ))}
            </div>
          </div>

          <div className="form-group">
            <label>Generated Secure RS256 JWT Token</label>
            <div className="token-preview">{jwtToken || 'Generating token...'}</div>
          </div>

          <div className="form-group" style={{ marginTop: '12px' }}>
            <label>Backend API Base URL</label>
            <input 
              type="text" 
              className="input-control" 
              value={backendUrl} 
              onChange={e => setBackendUrl(e.target.value)} 
            />
          </div>
        </aside>

        {/* Right Column: Endpoint Tester */}
        <main className="tester-panel glass-panel">
          
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ margin: 0 }}>Endpoint Execution Console</h3>
            {isUsingMock && (
              <span style={{ fontSize: '0.8rem', color: 'var(--accent-amber)', background: 'rgba(245, 158, 11, 0.15)', padding: '4px 8px', borderRadius: '4px', border: '1px solid rgba(245, 158, 11, 0.3)' }}>
                ⚠ Connection Failed: Running Mock Fallback
              </span>
            )}
          </div>

          {/* Resource tab selections */}
          {metadata && (
            <div className="resource-tabs">
              {Object.keys(metadata).map(resourceKey => (
                <button
                  key={resourceKey}
                  className={`tab ${selectedResource === resourceKey ? 'active' : ''}`}
                  onClick={() => {
                    setSelectedResource(resourceKey);
                    setQueryFilters([]);
                    setSingleId('');
                  }}
                >
                  {resourceKey.toUpperCase()} ({metadata[resourceKey].version})
                </button>
              ))}
            </div>
          )}

          {/* Operation & Tester Panel */}
          {currentResource && (
            <div className="operations-grid">
              
              {/* Config column */}
              <div className="op-config">
                <div className="op-tabs">
                  {(['GET_ALL', 'GET_BY_ID', 'POST', 'PUT', 'DELETE'] as const).map(op => {
                    const label = op.replace('_', ' ');
                    const activeClass = selectedOp === op ? `active ${op.split('_')[0].toLowerCase()}` : '';
                    return (
                      <button
                        key={op}
                        className={`op-tab ${activeClass}`}
                        onClick={() => setSelectedOp(op)}
                      >
                        {label}
                      </button>
                    );
                  })}
                </div>

                {/* Specific Config Parameters */}
                {selectedOp === 'GET_ALL' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <div style={{ display: 'flex', gap: '12px' }}>
                      <div className="form-group" style={{ flex: 1 }}>
                        <label>Page Index</label>
                        <input 
                          type="number" 
                          className="input-control" 
                          value={page} 
                          min="0"
                          onChange={e => setPage(Math.max(0, parseInt(e.target.value) || 0))} 
                        />
                      </div>
                      <div className="form-group" style={{ flex: 1 }}>
                        <label>Page Size</label>
                        <input 
                          type="number" 
                          className="input-control" 
                          value={size} 
                          min="1"
                          onChange={e => setSize(Math.max(1, parseInt(e.target.value) || 10))} 
                        />
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '12px' }}>
                      <div className="form-group" style={{ flex: 2 }}>
                        <label>Sort Attribute</label>
                        <select 
                          className="input-control"
                          value={sortField}
                          onChange={e => setSortField(e.target.value)}
                        >
                          <option value="id">id</option>
                          {currentResource.fields.map(f => (
                            <option key={f.name} value={f.name}>{f.name}</option>
                          ))}
                        </select>
                      </div>
                      <div className="form-group" style={{ flex: 1 }}>
                        <label>Direction</label>
                        <select 
                          className="input-control"
                          value={sortOrder}
                          onChange={e => setSortOrder(e.target.value as 'asc' | 'desc')}
                        >
                          <option value="asc">Ascending</option>
                          <option value="desc">Descending</option>
                        </select>
                      </div>
                    </div>

                    {/* Dynamic Filters Builder */}
                    <div className="form-group">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                        <label>Dynamic Specifications Filters</label>
                        <button className="btn-secondary" style={{ padding: '4px 10px', fontSize: '0.75rem' }} onClick={addFilter}>
                          + Add Filter
                        </button>
                      </div>
                      
                      {queryFilters.length === 0 ? (
                        <div style={{ padding: '12px', border: '1px dashed var(--panel-border)', borderRadius: '8px', textAlign: 'center', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          No filters active. Full tenant dataset will be matched.
                        </div>
                      ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                          {queryFilters.map((filter, index) => (
                            <div key={index} className="filter-row">
                              <select
                                className="input-control"
                                style={{ flex: 3 }}
                                value={filter.field}
                                onChange={e => handleFilterChange(index, 'field', e.target.value)}
                              >
                                {currentResource.fields.map(f => (
                                  <option key={f.name} value={f.name}>{f.name}</option>
                                ))}
                              </select>
                              
                              <select
                                className="input-control"
                                style={{ flex: 2 }}
                                value={filter.operator}
                                onChange={e => handleFilterChange(index, 'operator', e.target.value as any)}
                              >
                                <option value="=">Equals</option>
                                <option value="_like">Like</option>
                                <option value="_gt">Greater Than</option>
                                <option value="_lt">Less Than</option>
                                <option value="_gte">GTE</option>
                                <option value="_lte">LTE</option>
                              </select>
                              
                              <input
                                type="text"
                                className="input-control"
                                style={{ flex: 4 }}
                                placeholder="Match value..."
                                value={filter.value}
                                onChange={e => handleFilterChange(index, 'value', e.target.value)}
                              />
                              
                              <button className="filter-btn-remove" onClick={() => removeFilter(index)}>✕</button>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {(selectedOp === 'GET_BY_ID' || selectedOp === 'PUT' || selectedOp === 'DELETE') && (
                  <div className="form-group">
                    <label>Target Resource ID (Long)</label>
                    <input
                      type="text"
                      className="input-control"
                      placeholder="e.g. 1"
                      value={singleId}
                      onChange={e => setSingleId(e.target.value)}
                    />
                  </div>
                )}

                {(selectedOp === 'POST' || selectedOp === 'PUT') && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    <h4 style={{ margin: '8px 0 0 0', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Resource Body Fields (Record DTO Schema)</h4>
                    {currentResource.fields.map(field => {
                      const isRequired = field.required;
                      const hasConstraints = Object.keys(field.constraints).length > 0;
                      return (
                        <div key={field.name} className="form-group">
                          <label style={{ display: 'flex', alignItems: 'center' }}>
                            {field.name} ({field.type})
                            {isRequired && <span className="constraint-badge required">Required</span>}
                            {hasConstraints && (
                              <span className="constraint-badge">
                                {field.constraints.min !== undefined && `min: ${field.constraints.min} `}
                                {field.constraints.max !== undefined && `max: ${field.constraints.max} `}
                                {field.constraints.positive && `positive `}
                              </span>
                            )}
                          </label>
                          {field.type === 'Map' ? (
                            <textarea
                              className="input-control"
                              style={{ fontFamily: 'monospace', fontSize: '0.8rem', resize: 'vertical' }}
                              rows={3}
                              value={typeof formData[field.name] === 'object' ? JSON.stringify(formData[field.name]) : formData[field.name]}
                              onChange={e => handleFormChange(field.name, e.target.value, field.type)}
                            />
                          ) : field.type === 'Boolean' ? (
                            <select
                              className="input-control"
                              value={String(formData[field.name] ?? false)}
                              onChange={e => handleFormChange(field.name, e.target.value, field.type)}
                            >
                              <option value="false">false</option>
                              <option value="true">true</option>
                            </select>
                          ) : (
                            <input
                              type={field.type === 'Double' || field.type === 'Float' || field.type === 'Integer' || field.type === 'Long' ? 'number' : 'text'}
                              className="input-control"
                              value={formData[field.name] ?? ''}
                              onChange={e => handleFormChange(field.name, e.target.value, field.type)}
                            />
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}

                <button 
                  className="btn-primary" 
                  style={{ marginTop: '12px' }}
                  onClick={executeRequest}
                >
                  🚀 Execute Query Call
                </button>
              </div>

              {/* Console/Response column */}
              <div className="op-console">
                <div className="console-header">
                  <div className="console-title">NETWORK CONSOLE</div>
                  {consoleRes.status > 0 && (
                    <div className={`status-badge ${consoleRes.isError ? 'error' : 'success'}`}>
                      {consoleRes.status} {consoleRes.statusText}
                    </div>
                  )}
                  {consoleRes.isPending && (
                    <div className="status-badge pending">PENDING</div>
                  )}
                </div>

                {consoleReq.url && (
                  <div className="console-request-meta">
                    <div><strong>Request URI:</strong> {consoleReq.method} {consoleReq.url}</div>
                    {consoleRes.headers['x-request-id'] && (
                      <div><strong>Trace Request ID:</strong> {consoleRes.headers['x-request-id']}</div>
                    )}
                    {consoleRes.headers['x-total-count'] && (
                      <div><strong>Total Elements Header:</strong> {consoleRes.headers['x-total-count']}</div>
                    )}
                  </div>
                )}

                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <div className="console-title" style={{ fontSize: '0.75rem' }}>RESPONSE BODY</div>
                  {consoleRes.isPending ? (
                    <div className="console-response-body empty-text">Sending asynchronous HTTP payload to Spring WebFlux event loop...</div>
                  ) : consoleRes.body ? (
                    <div className={`console-response-body ${consoleRes.isError ? 'error-text' : ''}`}>
                      {consoleRes.body}
                    </div>
                  ) : (
                    <div className="console-response-body empty-text">Execute a request call to see HTTP response body stream output.</div>
                  )}
                </div>
              </div>

            </div>
          )}

        </main>

      </div>

      {/* 4. Core Technologies Section */}
      <section className="tech-stack-section glass-panel">
        <h3>🛠️ Core Technology Stack & Library Blueprint</h3>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '20px' }}>
          This framework orchestrates dynamic, runtime-driven CRUD endpoints using high-performance open-source libraries. Below is the list of frameworks/libraries in use and their roles:
        </p>
        <div className="tech-grid">
          <div className="tech-card">
            <h5>Spring Boot & WebFlux</h5>
            <p>Runs the high-concurrency Netty web server, utilizing reactive event loops to route requests non-blockingly, supporting correlation tracing and backpressure controls.</p>
          </div>
          <div className="tech-card">
            <h5>Byte Buddy (1.17.7)</h5>
            <p>Enables compile-free Java bytecode manipulation at runtime, dynamically compiling and inserting actual Spring RestController classes into the ApplicationContext during bootstrap.</p>
          </div>
          <div className="tech-card">
            <h5>Hibernate & JPA</h5>
            <p>Object-relational mapping and entity persistence framework. Decoupled from WebFlux event loop threads by scheduling operations on a transaction-scoped boundedElastic context.</p>
          </div>
          <div className="tech-card">
            <h5>PostgreSQL RLS</h5>
            <p>Multi-tenancy isolation layer using native Row-Level Security (RLS) policies. Enforces strict logical boundaries inside SQL transactions using app.current_tenant context variables.</p>
          </div>
          <div className="tech-card">
            <h5>Liquibase (5.0.3)</h5>
            <p>Declarative, version-controlled database schema migrations engine, applying reproducible changesets (tables, relationships, and RLS policies) automatically on startup.</p>
          </div>
          <div className="tech-card">
            <h5>Jackson 3 (Databind)</h5>
            <p>Enterprise JSON parsing engine configured with global XSS input sanitizers to strip dangerous script tags and strict deserializers that reject unwhitelisted schema fields.</p>
          </div>
          <div className="tech-card">
            <h5>JJWT (0.13.0)</h5>
            <p>Cryptographic JSON Web Token decoder. Parses Keycloak credentials and validates signature claims utilizing RS256 public key certificates cached from a JWKS endpoint.</p>
          </div>
          <div className="tech-card">
            <h5>React 19 & Vite 6</h5>
            <p>Modern frontend framework and ultra-fast build pipeline. Leverages the browser-native Web Crypto API to sign standard OIDC-compliant JWT authorization tokens on the fly.</p>
          </div>
        </div>

        <div className="hello-world-guide">
          <h4>💡 "Hello, World" Framework Extension Walkthrough</h4>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '16px', lineHeight: '1.45' }}>
            Adding a new dynamic API endpoint and database resource in this framework requires zero boilerplate. Follow this simple checklist:
          </p>
          <div className="hello-world-steps">
            <div className="step-item">
              <strong>Step 1: Create a JPA Entity</strong>
              Create a new entity class (e.g. <code>Device.java</code>) extending <code>BaseEntity</code>. Annotate it with <code>@Entity</code> and <code>@CrudResource(path = "devices", dto = DeviceRecord.class, roles = {"{"}"ADMIN", "USER"{"}"})</code>.
            </div>
            <div className="step-item">
              <strong>Step 2: Define a DTO Record</strong>
              Create a Java record (e.g. <code>DeviceRecord.java</code>) with field validation annotations (like <code>@NotBlank</code>). Map it back to the database entity using <code>@EntityMapping(entity = Device.class)</code>.
            </div>
            <div className="step-item">
              <strong>Step 3: Setup Liquibase Changeset</strong>
              Write a changeset in the master XML to create your table, map its primary key back to the parent table with a foreign key, and execute the SQL script to enable PostgreSQL Row-Level Security (RLS).
            </div>
            <div className="step-item">
              <strong>Step 4: Launch and Test</strong>
              Restart the backend! The framework dynamically compiles the new RestController and routes at <code>/api/v1/devices</code>. This frontend console will automatically fetch the new metadata and generate forms.
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

export default App;
