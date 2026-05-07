import { useState, useRef, useEffect } from "react";

// ─────────────────────────────────────────────────────────────
// API CONFIG
// In development, your Spring Boot server runs on 8080.
// In production, replace with your actual backend URL.
// Using an environment variable is the right pattern for this —
// in Create React App: REACT_APP_API_URL, in Vite: VITE_API_URL
// ─────────────────────────────────────────────────────────────
const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

// ─────────────────────────────────────────────────────────────
// EMPTY FORM — used when creating a brand new listing
// ─────────────────────────────────────────────────────────────
const EMPTY_FORM = {
  address: "", cityStateZip: "", neighborhood: "", price: "",
  style: "", yearBuilt: "", beds: "", baths: "", sqft: "",
  lotSqft: "", garage: "", taxes: "", estimatedPayment: "",
  agentName: "Jim", description: "", features: "", location: "",
};

// ─────────────────────────────────────────────────────────────
// BUILD SYSTEM PROMPT — same as before
// ─────────────────────────────────────────────────────────────
const buildSystemPrompt = (f) => {
  const featureList = f.features.split("\n").filter(Boolean).join("; ");
  const locationList = f.location.split("\n").filter(Boolean).join("; ");
  return `You are an AI listing assistant for a specific real estate property listed by ${f.agentName || "the listing agent"}.
  Do not use Markdown formatting in your responses. No bold, no bullets, no headers — plain conversational text only.
Answer questions from prospective buyers accurately, warmly, and concisely — 2-4 sentences unless more detail is genuinely needed.
If something is not covered in the details below, say so honestly. Never invent facts or numbers.
For showings or detailed inquiries, direct buyers to contact ${f.agentName || "the listing agent"} directly.

PROPERTY: ${f.address}, ${f.cityStateZip}
Neighborhood: ${f.neighborhood} | Price: ${f.price}
Style: ${f.style} | Year Built: ${f.yearBuilt}
Beds: ${f.beds} | Baths: ${f.baths} | Sq Ft: ${f.sqft}
Lot: ${f.lotSqft} sq ft | Garage: ${f.garage}

DESCRIPTION: ${f.description}
FEATURES: ${featureList}
LOCATION: ${locationList}
ESTIMATED COSTS: Property taxes: ${f.taxes}. Mortgage: ${f.estimatedPayment}.`.trim();
};

// ─────────────────────────────────────────────────────────────
// SHARED STYLES
// ─────────────────────────────────────────────────────────────
const S = {
  bg: "#faf7f2", bgCard: "#ffffff", bgDark: "#2c2416", accent: "#8a6a3a",
  border: "#e0d8cc", textPrimary: "#1a1612", textMuted: "#7a7060", textLight: "#f5f0e8",
  input: {
    width: "100%", fontSize: 13, padding: "7px 10px", borderRadius: 7,
    border: "1px solid #e0d8cc", background: "#faf7f2", color: "#1a1612",
    fontFamily: "inherit", boxSizing: "border-box",
  },
};

// ─────────────────────────────────────────────────────────────
// FIELD COMPONENT
// ─────────────────────────────────────────────────────────────
function Field({ label, value, onChange, multiline = false, rows = 3, half = false }) {
  const sharedStyle = { ...S.input, ...(multiline ? { resize: "vertical" } : {}) };
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 4, width: half ? "calc(50% - 4px)" : "100%" }}>
      <label style={{ fontSize: 11, fontWeight: 600, color: S.textMuted, textTransform: "uppercase", letterSpacing: "0.06em" }}>
        {label}
      </label>
      {multiline
        ? <textarea value={value} onChange={e => onChange(e.target.value)} rows={rows} style={sharedStyle} />
        : <input value={value} onChange={e => onChange(e.target.value)} style={sharedStyle} />
      }
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// CHAT COMPONENTS
// ─────────────────────────────────────────────────────────────
function ChatMessage({ role, content }) {
  const isUser = role === "user";
  return (
    <div style={{
      display: "flex", flexDirection: "column", gap: 3, maxWidth: "88%",
      alignItems: isUser ? "flex-end" : "flex-start",
      alignSelf: isUser ? "flex-end" : "flex-start",
    }}>
      <span style={{ fontSize: 10, color: S.textMuted, padding: "0 4px" }}>
        {isUser ? "You" : "Listing AI"}
      </span>
      <div style={{
        fontSize: 13, lineHeight: 1.6, padding: "8px 12px",
        borderRadius: isUser ? "10px 10px 2px 10px" : "10px 10px 10px 2px",
        background: isUser ? S.bgDark : "#f5f0e8",
        color: isUser ? S.textLight : S.textPrimary,
        border: isUser ? "none" : `1px solid ${S.border}`,
      }}>
        {content}
      </div>
    </div>
  );
}

function TypingIndicator() {
  return (
    <div style={{
      display: "flex", gap: 4, padding: "8px 12px", background: "#f5f0e8",
      borderRadius: "10px 10px 10px 2px", border: `1px solid ${S.border}`, alignSelf: "flex-start",
    }}>
      {[0, 1, 2].map(i => (
        <div key={i} style={{
          width: 5, height: 5, borderRadius: "50%", background: S.textMuted,
          animation: "bounce 1s infinite", animationDelay: `${i * 0.15}s`,
        }} />
      ))}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// MAIN APP
// ─────────────────────────────────────────────────────────────
export default function ListingConfigurator() {
  const [listings, setListings] = useState([]);         // all listings from the API
  const [selectedId, setSelectedId] = useState(null);   // which listing is loaded
  const [form, setForm] = useState(EMPTY_FORM);         // the editable form
  const [activePrompt, setActivePrompt] = useState(""); // committed AI prompt
  const [activeListing, setActiveListing] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [apiLoading, setApiLoading] = useState(false);  // loading state for backend calls
  const [isDirty, setIsDirty] = useState(false);
  const [mode, setMode] = useState("select");           // "select" | "edit" | "new"
  const [saveStatus, setSaveStatus] = useState("");     // feedback after save

  const messagesEndRef = useRef(null);

  // ── API CALLS ───────────────────────────────────────────────

  const loadListing = (listing) => {
    setSelectedId(listing.id);
    setForm(listing);
    setActivePrompt(buildSystemPrompt(listing));
    setActiveListing(listing);
    setMessages([]);
    setIsDirty(false);
    setMode("edit");
  };

  const fetchListings = async () => {
    setApiLoading(true);
    try {
      const res = await fetch(`${API_BASE}/listings`);
      const data = await res.json();
      setListings(data);
      if (data.length > 0) loadListing(data[0]);
    } catch {
      console.error("Could not reach backend. Is Spring Boot running on port 8080?");
    } finally {
      setApiLoading(false);
    }
  };

  const saveListing = async () => {
    setApiLoading(true);
    setSaveStatus("");
    try {
      const isNew = !form.id;
      const url = isNew ? `${API_BASE}/listings` : `${API_BASE}/listings/${form.id}`;
      const method = isNew ? "POST" : "PUT";
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });
      if (!res.ok) throw new Error(`Server returned ${res.status}`);
      const saved = await res.json();
      await fetchListings();
      loadListing(saved);
      setSaveStatus("Saved ✓");
    } catch (err) {
      setSaveStatus(`Save failed: ${err.message}`);
    } finally {
      setApiLoading(false);
    }
  };

  const deleteListing = async (id) => {
    if (!window.confirm("Delete this listing?")) return;
    await fetch(`${API_BASE}/listings/${id}`, { method: "DELETE" });
    setMode("select");
    setForm(EMPTY_FORM);
    setActiveListing(null);
    setActivePrompt("");
    setMessages([]);
    fetchListings();
  };

  const updateField = (field) => (value) => {
    setForm(prev => ({ ...prev, [field]: value }));
    setIsDirty(true);
    setSaveStatus("");
  };

  const configureAgent = () => {
    setActivePrompt(buildSystemPrompt(form));
    setActiveListing(form);
    setMessages([]);
    setIsDirty(false);
  };

  const sendMessage = async (text) => {
    if (!text.trim() || loading || !activePrompt) return;
    const userMessage = { role: "user", content: text };
    const updatedHistory = [...messages, userMessage];
    setMessages(updatedHistory);
    setInput("");
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          systemPrompt: activePrompt,
          messages: updatedHistory,
        }),
  });
      const data = await response.json();
      const reply = data.content[0].text;
      setMessages([...updatedHistory, { role: "assistant", content: reply }]);
    } catch {
      setMessages([...updatedHistory, { role: "assistant", content: "Sorry, something went wrong." }]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); sendMessage(input); }
  };

  // ── EFFECTS ───────────────────────────────────────────────
  useEffect(() => {
    fetchListings();
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  // ─────────────────────────────────────────────
  // RENDER
  // ─────────────────────────────────────────────
  return (
    <div style={{ fontFamily: "'Segoe UI', system-ui, sans-serif", background: S.bg, minHeight: "100vh", display: "flex", flexDirection: "column" }}>
      <style>{`
        @keyframes bounce { 0%,60%,100%{transform:translateY(0)} 30%{transform:translateY(-5px)} }
        input:focus, textarea:focus, select:focus { outline: none; border-color: ${S.accent} !important; }
        ::-webkit-scrollbar { width: 4px; }
        ::-webkit-scrollbar-thumb { background: #d0c8bc; border-radius: 4px; }
      `}</style>

      {/* TOP BAR */}
      <div style={{ background: S.bgDark, padding: "12px 1.5rem", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <div>
          <div style={{ fontSize: 15, fontWeight: 600, color: S.textLight }}>Listing Agent Builder</div>
          <div style={{ fontSize: 11, color: "#9a8e80" }}>Connected to Spring Boot API · {listings.length} listing{listings.length !== 1 ? "s" : ""} loaded</div>
        </div>
        <button
          onClick={() => { setForm(EMPTY_FORM); setMode("new"); setMessages([]); setActivePrompt(""); setActiveListing(null); }}
          style={{ fontSize: 12, padding: "6px 14px", borderRadius: 7, border: "1px solid rgba(255,255,255,0.2)", background: "transparent", color: S.textLight, cursor: "pointer", fontFamily: "inherit" }}
        >
          + New Listing
        </button>
      </div>

      {/* THREE-COLUMN LAYOUT */}
      <div style={{ display: "grid", gridTemplateColumns: "220px 1fr 1fr", flex: 1, height: "calc(100vh - 53px)" }}>

        {/* ═══ SIDEBAR: LISTING LIST ═══ */}
        <div style={{ borderRight: `1px solid ${S.border}`, overflowY: "auto", background: "#f5f1eb" }}>
          <div style={{ padding: "10px 12px", fontSize: 11, fontWeight: 700, color: S.textMuted, textTransform: "uppercase", letterSpacing: "0.07em", borderBottom: `1px solid ${S.border}` }}>
            Your Listings
          </div>
          {apiLoading && listings.length === 0 && (
            <div style={{ padding: "1rem", fontSize: 12, color: S.textMuted }}>Loading from API...</div>
          )}
          {!apiLoading && listings.length === 0 && (
            <div style={{ padding: "1rem", fontSize: 12, color: S.textMuted }}>No listings yet. Is Spring Boot running?</div>
          )}
          {listings.map(l => (
            <div
              key={l.id}
              onClick={() => loadListing(l)}
              style={{
                padding: "10px 12px", cursor: "pointer", borderBottom: `1px solid ${S.border}`,
                background: selectedId === l.id ? "#ede8e0" : "transparent",
                borderLeft: selectedId === l.id ? `3px solid ${S.accent}` : "3px solid transparent",
              }}
            >
              <div style={{ fontSize: 12, fontWeight: 600, color: S.textPrimary }}>{l.address}</div>
              <div style={{ fontSize: 11, color: S.textMuted, marginTop: 2 }}>{l.neighborhood} · {l.price}</div>
            </div>
          ))}
        </div>

        {/* ═══ FORM ═══ */}
        <div style={{ overflowY: "auto", padding: "1.25rem", borderRight: `1px solid ${S.border}` }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "1.25rem", flexWrap: "wrap", gap: 8 }}>
            <div>
              <div style={{ fontSize: 14, fontWeight: 600, color: S.textPrimary }}>
                {mode === "new" ? "New Listing" : form.address || "Listing Details"}
              </div>
              <div style={{ fontSize: 11, color: S.textMuted, marginTop: 2 }}>
                {mode === "new" ? "Fill in details and save to create." : "Edit below and save to backend or configure the agent."}
              </div>
            </div>
            <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
              {/* Save to backend */}
              <button onClick={saveListing} disabled={apiLoading} style={{
                fontSize: 12, padding: "7px 14px", borderRadius: 7, border: `1px solid ${S.border}`,
                background: "#ffffff", color: S.textPrimary, cursor: "pointer", fontFamily: "inherit",
              }}>
                {apiLoading ? "Saving..." : "Save to DB"}
              </button>
              {/* Configure the AI agent */}
              <button onClick={configureAgent} style={{
                fontSize: 12, padding: "7px 14px", borderRadius: 7, border: "none",
                background: isDirty ? S.accent : S.bgDark,
                color: S.textLight, cursor: "pointer", fontFamily: "inherit", fontWeight: 600,
              }}>
                {isDirty ? "⚡ Configure Agent" : "✓ Agent Ready"}
              </button>
              {/* Delete */}
              {form.id && (
                <button onClick={() => deleteListing(form.id)} style={{
                  fontSize: 12, padding: "7px 14px", borderRadius: 7, border: "1px solid #e0c0c0",
                  background: "transparent", color: "#c06060", cursor: "pointer", fontFamily: "inherit",
                }}>
                  Delete
                </button>
              )}
            </div>
          </div>

          {saveStatus && (
            <div style={{ fontSize: 12, color: saveStatus.includes("failed") ? "#c06060" : "#5a8a5a", marginBottom: 12, padding: "6px 10px", background: saveStatus.includes("failed") ? "#fff0f0" : "#f0fff0", borderRadius: 6, border: `1px solid ${saveStatus.includes("failed") ? "#f0c0c0" : "#c0e0c0"}` }}>
              {saveStatus}
            </div>
          )}

          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            <SectionLabel>Location &amp; Price</SectionLabel>
            <Field label="Street Address" value={form.address} onChange={updateField("address")} />
            <div style={{ display: "flex", gap: 8 }}>
              <Field label="City, State, ZIP" value={form.cityStateZip} onChange={updateField("cityStateZip")} half />
              <Field label="Neighborhood" value={form.neighborhood} onChange={updateField("neighborhood")} half />
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <Field label="List Price" value={form.price} onChange={updateField("price")} half />
              <Field label="Style / Type" value={form.style} onChange={updateField("style")} half />
            </div>

            <SectionLabel>Property Stats</SectionLabel>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <Field label="Beds" value={form.beds} onChange={updateField("beds")} half />
              <Field label="Baths" value={form.baths} onChange={updateField("baths")} half />
              <Field label="Sq Ft" value={form.sqft} onChange={updateField("sqft")} half />
              <Field label="Lot Sq Ft" value={form.lotSqft} onChange={updateField("lotSqft")} half />
              <Field label="Year Built" value={form.yearBuilt} onChange={updateField("yearBuilt")} half />
              <Field label="Garage" value={form.garage} onChange={updateField("garage")} half />
            </div>

            <SectionLabel>Description &amp; Features</SectionLabel>
            <Field label="Short Description" value={form.description} onChange={updateField("description")} multiline rows={3} />
            <Field label="Key Features (one per line)" value={form.features} onChange={updateField("features")} multiline rows={7} />
            <Field label="Location Highlights (one per line)" value={form.location} onChange={updateField("location")} multiline rows={3} />

            <SectionLabel>Costs &amp; Agent</SectionLabel>
            <Field label="Annual Property Taxes" value={form.taxes} onChange={updateField("taxes")} />
            <Field label="Estimated Mortgage Payment" value={form.estimatedPayment} onChange={updateField("estimatedPayment")} />
            <Field label="Your Name" value={form.agentName} onChange={updateField("agentName")} />
          </div>
        </div>

        {/* ═══ CHAT ═══ */}
        <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
          <div style={{ background: S.bgDark, padding: "12px 1.25rem" }}>
            <div style={{ fontSize: 14, fontWeight: 600, color: S.textLight }}>
              {activeListing ? activeListing.address : "No listing configured"}
            </div>
            <div style={{ fontSize: 11, color: "#9a8e80", marginTop: 1 }}>
              {activeListing ? `${activeListing.neighborhood} · ${activeListing.price} · AI listing assistant` : "Configure an agent to start chatting"}
            </div>
          </div>

          <div style={{ flex: 1, overflowY: "auto", padding: "1rem", display: "flex", flexDirection: "column", gap: 10, background: S.bg }}>
            {!activePrompt && (
              <div style={{ fontSize: 13, color: S.textMuted, textAlign: "center", marginTop: "2rem" }}>
                Select a listing and click "Configure Agent" to activate the AI assistant.
              </div>
            )}
            {activePrompt && messages.length === 0 && (
              <>
                <div style={{ alignSelf: "flex-start", maxWidth: "88%", display: "flex", flexDirection: "column", gap: 3 }}>
                  <span style={{ fontSize: 10, color: S.textMuted, padding: "0 4px" }}>Listing AI</span>
                  <div style={{ fontSize: 13, lineHeight: 1.6, padding: "8px 12px", borderRadius: "10px 10px 10px 2px", background: "#f5f0e8", color: S.textPrimary, border: `1px solid ${S.border}` }}>
                    Hi! I'm the AI assistant for {activeListing?.address}. Ask me anything about the property.
                  </div>
                </div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: 5, alignSelf: "flex-start" }}>
                  {["What are the monthly costs?", "Tell me about the neighborhood", "What's been updated?", "How's the storage?"].map((q, i) => (
                    <button key={i} onClick={() => sendMessage(q)} style={{ fontSize: 11, padding: "4px 9px", borderRadius: 6, border: `1px solid ${S.border}`, background: "transparent", color: S.textMuted, cursor: "pointer", fontFamily: "inherit" }}>
                      {q}
                    </button>
                  ))}
                </div>
              </>
            )}
            {messages.map((msg, i) => <ChatMessage key={i} role={msg.role} content={msg.content} />)}
            {loading && <TypingIndicator />}
            <div ref={messagesEndRef} />
          </div>

          <div style={{ padding: 10, background: "#f0ece4", borderTop: `1px solid ${S.border}`, display: "flex", gap: 6 }}>
            <textarea
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={activePrompt ? "Ask anything about this listing..." : "Configure an agent first..."}
              disabled={!activePrompt}
              rows={1}
              style={{ ...S.input, flex: 1, resize: "none", opacity: activePrompt ? 1 : 0.5 }}
            />
            <button
              onClick={() => sendMessage(input)}
              disabled={loading || !input.trim() || !activePrompt}
              style={{ fontSize: 12, padding: "7px 16px", borderRadius: 7, border: "none", background: S.bgDark, color: S.textLight, cursor: "pointer", fontFamily: "inherit", opacity: (loading || !activePrompt) ? 0.5 : 1 }}
            >
              {loading ? "..." : "Send"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// Small helper to avoid repeating section label styling
function SectionLabel({ children }) {
  return (
    <div style={{
      fontSize: 11, fontWeight: 700, color: "#8a6a3a",
      textTransform: "uppercase", letterSpacing: "0.08em",
      paddingBottom: 4, borderBottom: "1px solid #e0d8cc", marginTop: 4,
    }}>
      {children}
    </div>
  );
}
