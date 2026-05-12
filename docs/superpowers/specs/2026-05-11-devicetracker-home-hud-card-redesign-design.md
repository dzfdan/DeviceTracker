# DeviceTracker Home HUD Card Redesign Design

## Objective

Redesign the homepage `在线设备` and `轨迹记录` floating cards so they occupy less map space, use consistent visual height, and better support the map-first experience of the homepage.

## Problem

The current homepage top overlay uses a horizontal two-card layout:

- The `在线设备` card is too large and visually heavy
- The `轨迹记录` card has a different perceived height and density
- Together they occupy too much vertical and horizontal area over the map
- The map is no longer the clear primary surface on first view

This weakens the intended live-command-center feel of the homepage.

## Chosen Direction

Adopt the previously explored `方案 B` direction and refine it further:

- Convert the two top cards into a left-aligned vertical intelligence strip
- Reduce the strip width by roughly 30 percent compared with the original `方案 B` concept
- Keep the existing dark glass futuristic visual language
- Shift the cards from “dashboard panels” to “compact tactical HUD status modules”

## Layout Strategy

### Positioning

- Keep the app title and network label at the top-left as they are conceptually today
- Place the two floating status cards beneath the title area in a vertical stack
- Anchor the stack to the left side of the map overlay
- Avoid occupying the center-top and top-right map area

### Width

- Constrain the new vertical stack to approximately `128dp` to `138dp`
- This width is intentionally narrower than the prior concept and much narrower than the current combined horizontal layout
- The reduced width is a core design requirement, not an implementation suggestion

### Height and Consistency

- Both cards must feel visually aligned and intentionally compact
- The `在线设备` card should sit around `72dp` to `80dp`
- The `轨迹记录` card should sit around `64dp` to `72dp`
- The difference between the two may exist, but should feel controlled and harmonious rather than accidental
- Card spacing should be reduced to roughly `8dp` to `10dp`

## Card Content Model

### Online Devices Card

Purpose:

- Show the primary fleet status at a glance

Content hierarchy:

- Eyebrow label: `在线设备`
- Primary metric: device count
- Secondary line: short freshness/status text such as `8 秒前更新`

Rules:

- Remove the large, display-like metric treatment currently used
- Keep the count visually important, but tighter and more compact
- The secondary line must remain short enough to preserve card density

### Track Record Card

Purpose:

- Show route-tracking readiness or summary without competing with the map

Content hierarchy:

- Eyebrow label: `轨迹记录`
- Primary summary line: compact route information such as `84 点 / 2.6 km`

Rules:

- Avoid multi-line verbose text in the compact state
- Prefer a terse single summary line wherever possible
- If no route data is available, the empty state copy should stay short and compact

## Visual Language

The homepage should continue to feel like `Midnight Control Glass`, but this redesign sharpens the tone into a more tactical HUD.

Required visual characteristics:

- Dark translucent glass surfaces
- Thin luminous stroke treatment
- Reduced padding versus the current cards
- Tighter typography hierarchy
- Slightly deeper background on the track card to create subtle secondary emphasis

Not allowed:

- Large dashboard-style blocks
- Oversized metric typography that dominates the map
- Uneven padding causing mismatched apparent heights
- Decorative flourishes that increase visual weight without improving scan speed

## Implementation Boundaries

This redesign should stay tightly scoped to the homepage overlay cards.

Included:

- `activity_main.xml` layout restructuring for the two cards
- Supporting string usage if shorter copy or different labels are needed
- Minor style or spacing refinements required for the compact HUD presentation

Not included:

- A full homepage redesign
- Reworking FAB placement unless necessary to preserve spacing balance
- Changing the map implementation
- Altering homepage business logic or metrics behavior

## Success Criteria

The redesign is successful when:

- The two cards no longer dominate the map viewport
- Their heights feel intentionally aligned and visually consistent
- The left-side stacked HUD feels compact and deliberate
- The map regains clear visual priority on the homepage
- The cards remain readable at a glance and preserve key status information

## Risks

- Over-compressing the cards may hurt readability
- Reducing width too aggressively may force awkward text wrapping
- Keeping current strings unchanged may still make the compact layout feel crowded

## Mitigations

- Use shorter summaries in the compact card state
- Keep secondary text to one short line
- Tune padding, text size, and line count together instead of changing only width
