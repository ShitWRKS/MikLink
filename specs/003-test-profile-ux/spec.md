# Feature Specification: Test Profile Tabbed UX

**Feature Branch**: `develop`

**Created**: 2026-08-11

**Status**: Draft

**Input**: User description: "Reorganize Test Profile creation and editing into General, Link, Ping, and Speed test tabs, with test-specific thresholds and dynamic explanatory previews, while preserving the existing domain, validation, persistence, and test behavior."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configure a profile by test area (Priority: P1)

As a MikLink user, I can create or edit a test profile through four clearly named areas so general details and each test's settings are easy to locate without a separate thresholds area.

**Why this priority**: This is the core usability improvement and must preserve the complete profile workflow.

**Independent Test**: Create a profile, enter general details, configure Link and Ping values across multiple tabs, return to prior tabs without saving, save, reopen, edit, save again, and delete the profile through the visible UI.

**Acceptance Scenarios**:

1. **Given** the create or edit screen, **When** it opens, **Then** exactly four tabs are available in this order: General, Link, Ping, Speed test.
2. **Given** unsaved values in any tab, **When** the user changes tabs and returns, **Then** all entered values and test toggles remain unchanged and no implicit save occurs.
3. **Given** a valid profile, **When** the user saves it and later reopens it, **Then** all edited fields show the persisted values and the existing post-save navigation is preserved.
4. **Given** all test toggles are disabled, **When** the user views the persistent save action from any tab, **Then** the reason the action is unavailable is stated nearby.

---

### User Story 2 - Configure Link capabilities and threshold (Priority: P1)

As a MikLink user, I can enable Link Status, TDR, and LLDP/Neighbor Discovery together and configure the Link Status minimum rate in the same area.

**Why this priority**: Link settings currently span the general form and global thresholds, obscuring their relationship.

**Independent Test**: Open the Link tab, toggle each existing Link capability, select a preset rate, enter a valid custom rate, and confirm the threshold explanation updates without restricting the custom value.

**Acceptance Scenarios**:

1. **Given** the Link tab, **When** it is displayed, **Then** Link Status, TDR, and LLDP/Neighbor Discovery appear together with their existing toggles and TDR supporting text.
2. **Given** Link Status settings, **When** a user selects one of the ten existing presets or enters a valid custom rate, **Then** the effective field value is retained without turning the preset list into a domain limit.
3. **Given** a valid Link minimum rate, **When** it changes, **Then** an explanatory discrete-rate visualization immediately identifies the selected value and communicates that negotiated speed passes at or above the threshold.
4. **Given** a valid custom value outside the preset set, **When** it is entered, **Then** its numeric value remains visible and the visualization degrades gracefully without clamping or changing it.

---

### User Story 3 - Configure Ping execution and quality thresholds (Priority: P1)

As a MikLink user, I can configure Ping targets, execution count, gateway behavior, local thresholds, and external thresholds in one area with a clear preview of the RTT limits.

**Why this priority**: Ping has the most configuration and must remain understandable without hidden or duplicated state.

**Independent Test**: Enable Ping, use quick fill, add and remove optional targets, edit the ping count and local/external thresholds, move between tabs, and observe the preview and validation behavior.

**Acceptance Scenarios**:

1. **Given** the Ping tab, **When** it is displayed, **Then** the Ping toggle, three target slots, optional target removal, Gateway/Google/Cloudflare quick fill, ping count, conditional gateway policy, and local/external threshold editors are available without an extra collapsible Ping configuration layer.
2. **Given** Ping is disabled, **When** the tab remains open, **Then** its configuration stays visible and editable and no existing value is erased.
3. **Given** at least one target is `DHCP_GATEWAY`, **When** target values change, **Then** the existing unresolved-gateway policy is shown only while that condition remains true.
4. **Given** local or external Ping thresholds, **When** they are edited, **Then** maximum loss percentage, maximum average RTT, and maximum RTT remain the only Ping threshold fields.
5. **Given** valid or blank RTT threshold inputs, **When** they change, **Then** a clearly labeled threshold preview immediately shows the maximum average RTT and maximum RTT references using a deterministic illustrative series; blank values use existing effective defaults without changing the input.
6. **Given** an invalid RTT threshold input, **When** the preview updates, **Then** the field error remains visible and any preview portion that cannot be calculated is omitted or degraded without inventing a value or crashing.
7. **Given** packet loss thresholds, **When** the RTT preview is shown, **Then** packet loss remains a separate numeric control and is not placed on the RTT axis.

---

### User Story 4 - Configure Speed test thresholds (Priority: P2)

As a MikLink user, I can enable Speed test and configure all existing throughput and quality limits in one area with an immediate visual comparison of download and upload minima.

**Why this priority**: Speed settings need the same local organization and explanatory feedback as Link and Ping.

**Independent Test**: Edit all five existing Speed test thresholds, disable and re-enable the test without losing values, and observe the throughput comparison and separate quality indicators.

**Acceptance Scenarios**:

1. **Given** the Speed test tab, **When** it is displayed, **Then** the existing toggle and exactly five thresholds are available: maximum ping, maximum jitter, maximum packet loss, minimum download, and minimum upload.
2. **Given** Speed test is disabled, **When** threshold values are edited or the user changes tabs, **Then** all configuration remains available and unchanged.
3. **Given** valid or blank download/upload values, **When** either changes, **Then** a bar or scale comparison updates immediately from the effective values, uses a stable logarithmic slider capped at 100G (100,000 Mbps), and rejects manual values above that latest explicit limit.
4. **Given** ping, jitter, or packet-loss thresholds, **When** the throughput visualization is shown, **Then** those metrics remain separate compact indicators and are not placed on the Mbps axis.

### Edge Cases

- A blank threshold input remains valid and uses the existing effective default for saving and previews without mutating the blank field.
- An invalid numeric, percentage, or Link rate input keeps its existing validation error, disables save, and cannot crash a preview.
- A valid custom Link rate outside the preset range remains accepted and numerically represented without clamping.
- Optional Ping targets can be shown, edited, removed, and recreated without affecting other Ping settings.
- A pre-existing profile with any combination of enabled tests and persisted thresholds opens without migration and retains the same values.
- Configuration changes survive ordinary recomposition and configuration recreation during the edit session but are not stored as part of the profile until Save.
- A narrow display keeps all four tabs reachable by direct tab selection and provides independently scrollable content for the selected area.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The profile create/edit screen MUST retain its top app bar, Back navigation, persistent bottom Save action, create/edit behavior, validation, persistence, and post-save navigation.
- **FR-002**: The screen MUST expose exactly four tabs, ordered General, Link, Ping, and Speed test, and MUST NOT expose a Thresholds tab or global thresholds summary.
- **FR-003**: The selected tab MUST survive ordinary recomposition and configuration recreation during the edit session without being persisted as profile data.
- **FR-004**: Each tab MUST provide its own scrollable content, and changing tabs MUST NOT save, reset, recreate defaults, change toggles, or discard unsaved form input.
- **FR-005**: The General tab MUST contain only profile name and description.
- **FR-006**: When all tests are disabled, the global Save area MUST explain why Save is unavailable without introducing a separate error-summary system.
- **FR-007**: The Link tab MUST group the existing Link Status, TDR, and LLDP/Neighbor Discovery toggles, retain the existing TDR supporting text, and add no TDR or LLDP configuration.
- **FR-008**: The Link tab MUST expose the existing Link minimum rate field, all existing presets (10M, 100M, 1G, 2.5G, 5G, 10G, 25G, 40G, 50G, 100G), and valid custom-rate entry.
- **FR-009**: Link rate validation and unit interpretation MUST remain identical to the existing behavior, and presets MUST NOT constrain valid custom rates.
- **FR-010**: The Link tab MUST provide a dynamic discrete-threshold visualization, not a time series, that shows the actual minimum rate and communicates pass at negotiated speed greater than or equal to the threshold.
- **FR-011**: The Link visualization MUST preserve and display valid out-of-preset custom values without clamping or changing configuration.
- **FR-012**: The Ping tab MUST expose the existing Ping toggle, three target slots, optional-target removal, Gateway/Google/Cloudflare quick fill, ping count, conditional gateway policy, local thresholds, and external thresholds without an additional collapsible Ping configuration layer.
- **FR-013**: Ping configuration MUST remain visible and editable when Ping execution is disabled, and disabling Ping MUST NOT clear any value.
- **FR-014**: The unresolved-gateway policy MUST appear only when a target equals `DHCP_GATEWAY` and MUST retain its existing meaning.
- **FR-015**: Local and external Ping threshold editors MUST each contain only maximum loss percentage, maximum average RTT, and maximum RTT; no minimum RTT field or property may be introduced.
- **FR-016**: Ping MUST provide a dynamic, explicitly labeled threshold preview based on a deterministic illustrative series, showing only maximum average RTT and maximum RTT reference indicators.
- **FR-017**: Ping packet loss MUST remain a separate numeric control outside the RTT axis.
- **FR-018**: For preview purposes, blank Ping threshold inputs MUST use existing effective defaults without mutating the input; invalid inputs MUST retain field validation and cause only the uncomputable preview portion to degrade or disappear.
- **FR-019**: The Speed test tab MUST expose the existing toggle and exactly the five existing thresholds: maximum ping, maximum jitter, maximum loss percentage, minimum download, and minimum upload.
- **FR-020**: Speed test configuration MUST remain visible and editable when execution is disabled, and disabling Speed test MUST NOT clear any value.
- **FR-021**: Minimum download and upload MUST have a dynamic bar or comparable visualization plus a stable logarithmic slider from 0 through 100G (100,000 Mbps); the same upper bound MUST apply to manual input and save validation.
- **FR-022**: Speed ping, jitter, and loss MUST be shown as separate compact controls or indicators and MUST NOT share the Mbps axis.
- **FR-023**: All previews MUST be clearly presented as configuration aids rather than measured test results, MUST use no real data, and MUST NOT persist their illustrative data to profiles, storage, reports, or test execution.
- **FR-024**: Existing profile field ownership, defaults, parsing, validation, unit conversion, quality decisions, persistence types, and stored format MUST remain the source of truth and MUST NOT be duplicated or changed by the UI.
- **FR-025**: Existing profiles MUST open with all persisted values intact, and profiles saved through the new layout MUST round-trip the same fields without a schema or migration change.
- **FR-026**: Existing semantic identifiers MUST remain stable where their controls remain; the obsolete collapsible Ping identifier MUST be removed only after all consumers move to four stable locale-independent tab identifiers.
- **FR-027**: The functional profile UI journey MUST use stable identifiers to create, configure Link and Ping, verify unsaved state across tabs, save, reopen, edit, save again, reopen, delete through the UI, and verify removal.
- **FR-028**: Every new user-facing string MUST have compatible English and Italian resources, while canonical non-translatable content remains unchanged.
- **FR-029**: The feature MUST introduce no new dependency, database/schema/migration change, backup/report/test-execution change, global theme change, feature flag, or parallel legacy layout.
- **FR-030**: Documentation MUST accurately state that Client and Profile functional UI tests perform real UI deletion and MUST remove directly related descriptions of the obsolete profile form where present.

### Key Entities

- **Test Profile**: The existing persisted configuration containing profile details, enabled tests, Ping targets/count, and test thresholds. Its fields and format remain unchanged.
- **Link Threshold**: The existing optional minimum negotiated rate, interpreted by the existing strict rate rules.
- **Ping Threshold Set**: The existing local or external trio of maximum loss, maximum average RTT, and maximum RTT.
- **Speed Threshold Set**: The existing maximum ping, maximum jitter, maximum loss, minimum download, and minimum upload values.
- **Threshold Preview**: Transient, illustrative presentation derived from current inputs and existing defaults; it is not profile data or a test result.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can reach every profile field through exactly four named tabs and no Thresholds tab or global thresholds summary is present.
- **SC-002**: The create-edit-reopen-delete acceptance journey completes through visible controls while preserving every changed value across tab switches and save round trips.
- **SC-003**: Existing valid profile inputs, including Link custom rates and blank thresholds, produce the same saved values and validation outcomes except for the explicitly superseding 100G Download/Upload maximum requested on 2026-08-11.
- **SC-004**: Link, Ping, and Speed threshold previews update within the same interaction that changes a valid input, remain clearly labeled as illustrative, and never replace numeric field visibility.
- **SC-005**: All automated unit, quality, lint, debug build, and instrumentation-compilation gates required by the repository complete successfully; device-only checks report PASS only when actually executed on an authorized device.
- **SC-006**: The final change contains zero new dependencies, zero persistence/schema changes, zero domain or quality-policy changes, and zero unresolved CRITICAL or HIGH specification consistency findings.

## Assumptions

- The existing Test Profile ViewModel and domain models remain the authoritative form state, defaults, validation, parsing, and save path.
- Direct tab selection is sufficient; swipe navigation is outside scope.
- A deterministic illustrative series is sufficient to explain RTT references because previews are not measurements.
- Existing Material styling and theme tokens provide the intended appearance; global palette, typography, and shapes remain unchanged.
- Physical-device execution depends on one authorized, compatible Android device and must otherwise be reported as `NOT_RUN`.
