---
name: Lavender Pop Live
colors:
  surface: '#fdf7ff'
  surface-dim: '#ded6f4'
  surface-bright: '#fdf7ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f8f1ff'
  surface-container: '#f2ebff'
  surface-container-high: '#ede4ff'
  surface-container-highest: '#e7defd'
  on-surface: '#1d182f'
  on-surface-variant: '#484553'
  inverse-surface: '#322d45'
  inverse-on-surface: '#f5eeff'
  outline: '#797585'
  outline-variant: '#c9c4d6'
  surface-tint: '#5e49c8'
  primary: '#5b46c5'
  on-primary: '#ffffff'
  primary-container: '#7461e0'
  on-primary-container: '#fffbff'
  inverse-primary: '#c8bfff'
  secondary: '#7e5262'
  on-secondary: '#ffffff'
  secondary-container: '#fec5d8'
  on-secondary-container: '#7a4f5f'
  tertiary: '#5c5a69'
  on-tertiary: '#ffffff'
  tertiary-container: '#757382'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e5deff'
  primary-fixed-dim: '#c8bfff'
  on-primary-fixed: '#1a0063'
  on-primary-fixed-variant: '#452daf'
  secondary-fixed: '#ffd9e4'
  secondary-fixed-dim: '#efb7ca'
  on-secondary-fixed: '#31101e'
  on-secondary-fixed-variant: '#633b4a'
  tertiary-fixed: '#e4e0f2'
  tertiary-fixed-dim: '#c8c4d6'
  on-tertiary-fixed: '#1b1a27'
  on-tertiary-fixed-variant: '#474553'
  background: '#fdf7ff'
  on-background: '#1d182f'
  surface-variant: '#e7defd'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 34px
    fontWeight: '800'
    lineHeight: 42px
    letterSpacing: -0.03em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 30px
    letterSpacing: -0.02em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '700'
    lineHeight: 26px
    letterSpacing: -0.01em
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: 0em
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0em
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.75rem
  margin: 1.25rem
  margin-sm: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system embodies the electric, joyful sensation of live stage culture—from intimate indie gigs in Hongdae to stadium K-pop tours and musical theater revivals. It fuses the polished curation of trendsetting Korean lifestyle commerce (29CM, Zigzag) with the excitement of live ticketing platforms (Melon Ticket, Interpark).

### Brand Voice & Attributes
- **Playful yet Polished:** Youthful and energetic without descending into chaotic clutter. Every performance card, filter, and schedule badge is rendered with typographic discipline.
- **Spark & Delight:** Incorporates subtle sparkle motifs (✦), soft lavender-pink atmospheric glows, and tactile gesture-driven micro-interactions.
- **Effortless Discovery:** Translates complex performance schedules, seating tiers, and casting lineups into scan-friendly, digestible surfaces.

### Design Movement
- **Atmospheric Pastel Modernism:** A hybrid of contemporary Android Material 3 ergonomics and airy, pastel-tinted surfaces. Layered soft blurs, tinted ambient glows, and organic curved containers create a warm, inviting cultural hub.

## Colors

The palette embraces the "Lavender Pop" harmony, using tinted whites and deep aubergine-charcoal contrasts to avoid the sterile feel of monochrome grays.

### Functional Roles
- **Primary (`#8875F5`):** The signature electric lavender. Drives primary action buttons, key event tags, active navigation indicators, and spark accents.
- **Secondary Pink (`#FFC6D9`):** Blush accent pink. Used for live notification badges, heart/bookmark toggles, sold-out alert flashes, and gradient highlights paired with primary lavender.
- **Lavender Surface (`#F1EDFF`):** Pale violet container tone. Powers category chip backgrounds, non-active selection states, and secondary card highlights.
- **Background (`#FCFAFF`):** Airy lavender-tinted white base canvas that keeps screens soft on the eyes during prolonged browsing.
- **Surface (`#FFFFFF`):** Crisp pure white reserved for elevated cards, modal sheets, and sticky action surfaces.
- **Primary Text (`#29243B`):** Deep eggplant charcoal providing high-contrast readability (AAA against white) while retaining warmth.
- **Secondary Text (`#777483`):** Neutral violet-gray for dates, venue locations, cast descriptions, and inactive states.
- **Divider & Border (`#ECE8F3`):** Ultra-soft pastel border ensuring subtle visual separation without harsh demarcation lines.

## Typography

The type scale relies on modern geometric letterforms with open counters and approachable roundness. While Plus Jakarta Sans is designated for the token baseline, live Korean production layouts should bind `Pretendard Variable` as the direct font-family fallback to maintain uniform weight distribution, baseline alignment, and kerning pairs across Hangul, Latin, and numeric strings.

### Rules of Usage
- **Display & Section Titles:** Emphasize bold weights (700/800) with subtle negative letter spacing (`-0.02em` to `-0.03em`) to mimic Korean editorial headline treatments.
- **Performance Details:** Venues, dates, and ticket prices use `label-md` and `body-sm` with neutral secondary tone (`#777483`) to sustain breathing room around primary titles.
- **Numbers & Metrics:** Seat counters and D-day badges always pair bold weights with tabular figures (`tnum`) for flawless vertical alignment in schedules.

## Layout & Spacing

An Android-first layout architecture structured on an 8pt modular rhythm (with a 4pt sub-grid for badges and chip tags).

### Grid & Canvas Structure
- **Mobile Canvas (Default):** Fluid single-column system bounded by a standard outer horizontal padding of `margin` (20px / `1.25rem`). Horizontal carousels (e.g., "Trending Lineups", "Curated For You") bleed edge-to-edge with matching 20px leading and trailing inset padding.
- **Card Grids:** 2-column discovery grids utilize a strict 12px to 16px gutter (`gutter-sm` / `gutter`) ensuring artwork posters remain legible without cramped text.
- **Touch Targets:** Strictly preserves Material 3 touch ergonomics. Every interactive element maintains at least a 48×48dp interactive bounding area, even when visual chips and icon buttons measure 36dp.

## Elevation & Depth

Visual hierarchy rejects muddy, neutral gray drop shadows in favor of tinted ambient glow projections and subtle tonal layering.

### Depth Layers
- **Base Canvas:** `#FCFAFF` provides an airy, luminous floor.
- **Level 1 (Card & Section Surfaces):** Pure white (`#FFFFFF`) cards resting on the tinted base canvas with an ultra-soft violet ambient blur:
  - `box-shadow: 0 4px 20px rgba(136, 117, 245, 0.08);`
  - Subtle hairline stroke: `1px solid #ECE8F3` around cards prevents bleed onto white screens.
- **Level 2 (Modals, Sticky Navigation & Drawers):** Elevated sheets and bottom floating navigation bars:
  - `box-shadow: 0 10px 30px rgba(136, 117, 245, 0.16);`
  - Background: `rgba(255, 255, 255, 0.88)` with `backdrop-filter: blur(16px)` to achieve a soft glassmorphic frost.
- **Level 3 (Floating Action & Quick Booking Bar):** High-priority sticky CTAs utilize a soft colored drop shadow:
  - `box-shadow: 0 8px 24px rgba(136, 117, 245, 0.35);`

## Shapes

The design language balances approachable, friendly curves with high structural consistency.

### Corner Radii Guidelines
- **Event & Poster Cards:** Uses 16px to 20px radii (`rounded-2xl`), evoking the feel of collectible digital tickets and aesthetic lifestyle photo cards.
- **Buttons & Tags:** Key call-to-actions, category tags, artist badges, and filter pills use full-radius pill styling (`rounded-full`, 9999px).
- **Bottom Sheets & Drawers:** Top-left and top-right corners use 24px radii with a 4px thick centered drag handle.
- **Icon Enclosures:** Icon buttons and profile avatars leverage circular framing (rounded-full) or squircle soft enclosures (12px).

## Components

### Buttons
- **Primary CTA:** Full-width pill or 52dp height button. Filled with `#8875F5`, typography in white (`#FFFFFF`, `label-lg`). Includes the tinted violet shadow (`0 8px 20px rgba(136, 117, 245, 0.3)`).
- **Secondary / Action Ghost:** Surface `#F1EDFF` with `#8875F5` text. Used for secondary ticketing triggers like "Set Alarm" or "Add to Wishlist".
- **Floating Ticket Bar:** Sticky bottom bar with blurred background, dual layout (left: Price & Seat counter; right: "Book Now" primary pill button).

### Chips & Filter Pills
- **Unselected:** Background `#F1EDFF`, text `#777483`, no border, pill shape (`height: 36dp`).
- **Selected:** Background `#8875F5`, text `#FFFFFF`, soft glow shadow, optionally preceded by a sparkle glyph (`✦`).
- **Counter Chips:** Contain small `#FFC6D9` accent dots or live indicator pulses.

### Performance Cards
- **Poster Thumbnail:** 3:4 aspect ratio with `border-radius: 16px`, subtle inner stroke, and a top-right floating wishlist heart icon with glass backing.
- **Metadata Layout:** Category tag in `label-sm` (`#8875F5`), title in `headline-sm` (`#29243B`), date and venue in `body-sm` (`#777483`).
- **Status Badges:** "D-Day", "Almost Sold Out", or "Exclusive" tags positioned over the poster bottom or header with `#FFC6D9` background and `#29243B` text.

### Text Inputs & Search Bars
- **Search Header:** 48dp pill shape with background `#FFFFFF`, border `1px solid #ECE8F3`, leading magnifying glass icon in `#8875F5`, placeholder text in `#777483`.
- **Focused State:** Border transitions to `#8875F5` with a subtle `0 0 0 3px rgba(136, 117, 245, 0.15)` focus ring.

### Checkboxes, Radios & Switches
- **Tick Toggles:** Circular checks. When checked: `#8875F5` background with white check icon. When unchecked: `#ECE8F3` fill with neutral border.
- **Switches:** Android M3 track style. Active track `#8875F5`, thumb `#FFFFFF` with pink sparkle micro-state on toggle.

### Lists & Cast Carousels
- **Cast / Crew Avatars:** Circular 64dp portraits with a subtle 2px `#F1EDFF` border ring, centered artist name, and secondary role descriptor below.
- **Schedule List Items:** Divided by subtle `#ECE8F3` lines with date block on the left (e.g., `05.24 SAT`) and performance timing/booking status on the right.