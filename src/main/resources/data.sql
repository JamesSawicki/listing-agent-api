-- data.sql — Synthetic NorthStar MLS listings for development
-- 20 listings across all statuses, property types, and price points
-- Field order matches Listing.java field declaration order
-- All values reflect realistic Twin Cities MLS data shapes

-- Schema note: H2 requires column list when not inserting all columns.
-- Columns not listed receive NULL. TEXT columns handle multi-sentence strings.
-- Dates stored as ISO 8601 (YYYY-MM-DD) matching LocalDate field type in Listing.java
--
-- Type changes from Listing.java refactor (MLS Grid integration):
--   fireplaces        String -> Integer  (bare integer, no quotes)
--   association_fee   String -> Long     (bare integer, no quotes)
--   tax_amount        String -> Long     (bare integer, no quotes)
--   tax_year          String -> Integer  (bare integer, no quotes)
--   tax_with_assessments stays String    (keeps quotes: NST format '1069.0400')
--
-- New nullable columns added to entity (receive NULL from this file, populated by ingestion):
--   listing_key, mlg_can_view, modification_timestamp, original_entry_timestamp,
--   mlg_can_use, off_market_date, postal_city, new_construction, contingency,
--   parcel_number, zoning_description, school_district_number,
--   primary_image_url, media_json

INSERT INTO listings (
    mls_id, status, list_date, pending_date, close_date, days_on_market,
    list_price, original_list_price, close_price,
    address, city, zip_code, county, neighborhood, complex_subdiv,
    property_type, style, year_built, stories, construction_status,
    beds, baths_full, baths_three_quarter, baths_half, baths_quarter,
    sqft_above_grade, sqft_below_grade, sqft_total, sqft_main_level,
    lot_sqft, lot_dimensions, garage_stalls, garage_sqft, pool,
    room_info,
    appliances, basement, heating, air_conditioning, fuel_type,
    fireplace_features, fireplaces, construction_materials, exterior_features,
    roof, electric, sewer, water_source, fencing, lot_features,
    dining_room_features, family_room_features, amenities,
    parking_features, laundry_features, financing,
    waterfront_feet, waterfront_view, water_body_name, surface_water_type,
    dnr_lake_class, dnr_lake_id, lake_acres, lake_depth, lake_bottom_type,
    association_fee, association_fee_frequency, association_fee_includes,
    association_mgmt_name, association_mgmt_phone,
    tax_amount, tax_year, tax_with_assessments,
    elementary_school, middle_school, high_school, school_district,
    list_agent_name, list_agent_phone, list_agent_mls_id,
    list_office_name, list_office_phone, list_office_mls_id,
    public_remarks, photos_count, directions
) VALUES

-- 1. Active | Single Family | Edina | $875,000
(
    '7001001', 'Active', '2026-03-15', NULL, NULL, 22,
    875000, 875000, NULL,
    '5824 Interlachen Blvd', 'Edina', '55436', 'Hennepin', 'Interlachen Park', NULL,
    'Single Family', 'Two Story', 1987, 'Two', 'Previously Owned',
    5, 3, 1, 1, 0,
    2850, 1200, 4050, 1450,
    15246, '102x149', 3, 768, 'None',
    '[{"room":"Living Room","level":"Main","dim":"16x22"},{"room":"Dining Room","level":"Main","dim":"13x14"},{"room":"Kitchen","level":"Main","dim":"15x18"},{"room":"Family Room","level":"Main","dim":"16x20"},{"room":"Bedroom 1","level":"Upper","dim":"15x17"},{"room":"Bedroom 2","level":"Upper","dim":"12x13"},{"room":"Bedroom 3","level":"Upper","dim":"11x12"},{"room":"Bedroom 4","level":"Upper","dim":"11x12"},{"room":"Bedroom 5","level":"Lower","dim":"12x14"},{"room":"Recreation Room","level":"Lower","dim":"18x24"}]',
    'Air-To-Air Exchanger, Dishwasher, Disposal, Dryer, Microwave, Range, Refrigerator, Washer, Water Softener - Owned',
    'Drain Tiled, Finished (Livable), Full, Sump Pump',
    'Forced Air', 'Central', 'Natural Gas',
    'Gas Burning, Primary Bedroom', 2,
    'Brick/Stone, Cedar', 'Deck, In-Ground Sprinkler',
    'Asphalt Shingles', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    'Partial, Wood', 'Corner Lot, Tree Coverage - Medium',
    'Separate/Formal Dining Room', '2 or More, Family Room, Main Level',
    'Ceiling Fan(s), Deck, Hardwood Floors, In-Ground Sprinkler, Kitchen Center Island, Primary Bedroom Walk-In Closet, Vaulted Ceiling(s)',
    'Attached Garage, Driveway - Asphalt', 'Laundry Room, Lower Level', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    12450, 2026, '12450',
    'Highlands', 'Highlands', 'Edina', '273 - Edina',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Classic Interlachen Park two-story on a beautifully landscaped corner lot. Updated kitchen with quartz countertops, center island, and stainless appliances opens to a vaulted family room with gas fireplace. Main level features hardwood floors throughout, formal living and dining rooms, and a mudroom off the three-car garage. Upper level primary suite with walk-in closet and spa bath. Four additional bedrooms, plus a fully finished lower level with recreation room, fifth bedroom, and bath. In-ground sprinkler system, composite deck, mature trees. Edina schools.',
    42, 'From Hwy 100, east on Vernon Ave, south on Interlachen Blvd, home on right at corner of Interlachen and Schaefer Rd.'
),

-- 2. Active | Single Family | Wayzata | Lakefront | $3,200,000
(
    '7001002', 'Active', '2026-02-28', NULL, NULL, 37,
    3200000, 3200000, NULL,
    '1845 Ferndale Rd', 'Wayzata', '55391', 'Hennepin', NULL, NULL,
    'Single Family', 'Two Story', 2004, 'Two', 'Previously Owned',
    6, 4, 1, 1, 0,
    4600, 2200, 6800, 2300,
    43560, '100x435', 4, 1200, 'Below Ground, Heated, Outdoor',
    '[{"room":"Great Room","level":"Main","dim":"22x28"},{"room":"Kitchen","level":"Main","dim":"18x22"},{"room":"Dining Room","level":"Main","dim":"14x18"},{"room":"Office","level":"Main","dim":"12x14"},{"room":"Bedroom 1","level":"Main","dim":"18x22"},{"room":"Bedroom 2","level":"Upper","dim":"14x16"},{"room":"Bedroom 3","level":"Upper","dim":"13x15"},{"room":"Bedroom 4","level":"Upper","dim":"12x14"},{"room":"Bedroom 5","level":"Upper","dim":"12x13"},{"room":"Bedroom 6","level":"Lower","dim":"13x15"},{"room":"Recreation Room","level":"Lower","dim":"24x30"},{"room":"Exercise Room","level":"Lower","dim":"14x18"}]',
    'Air-To-Air Exchanger, Central Vacuum, Cooktop, Dishwasher, Disposal, Dryer, Exhaust Fan/Hood, Microwave, Refrigerator, Wall Oven, Washer, Water Filtration System, Water Softener - Owned, Wine Cooler',
    'Drain Tiled, Finished (Livable), Full, Storage Space, Walkout',
    'Forced Air, In-Floor Heating, Radiant', 'Central', 'Natural Gas',
    'Gas Burning, Primary Bedroom, Great Room', 3,
    'Stucco, Stone', 'Balcony, Deck, Patio, In-Ground Sprinkler',
    'Architectural Shingle', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    NULL, 'Accessible Shoreline, Tree Coverage - Light',
    'Informal Dining Room, Separate/Formal Dining Room', '2 or More, Great Room, Main Level, Lower Level',
    'Balcony, Deck, Exercise Room, Hardwood Floors, In-Ground Sprinkler, Kitchen Center Island, Panoramic View, Patio, Primary Bedroom Walk-In Closet, Sauna, Security System, Vaulted Ceiling(s), Wet Bar',
    'Attached Garage, Driveway - Asphalt, Heated Garage, Insulated Garage', 'Laundry Room, Main Level', 'Cash, Conventional',
    145, 'West, Lake', 'Lake Minnetonka', 'Lake',
    'General Development', '27013300', '14205.6', '113', 'Sand',
    NULL, NULL, NULL, NULL, NULL,
    28400, 2026, '28400',
    'Wayzata', 'Wayzata', 'Wayzata', '284 - Wayzata',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Rare opportunity to own 145 feet of pristine Lake Minnetonka frontage on coveted Ferndale Road. This custom-built estate delivers panoramic lake views from nearly every room. The grand great room features soaring ceilings, floor-to-ceiling windows, and a stone fireplace. Gourmet kitchen with Wolf and Sub-Zero appliances, large center island, and informal dining overlooking the water. Main-level primary suite with fireplace, walk-in closet, and spa bath with heated floors. Four additional upper-level bedrooms each with en-suite baths. Walkout lower level designed for entertaining — wet bar, recreation room, exercise room, sauna, and sixth bedroom. In-ground pool with automatic cover. Four-car heated garage. Orono schools.',
    61, 'From Wayzata Blvd, south on Ferndale Rd, home on right with lake views visible from street.'
),

-- 3. Active | Condo | Minneapolis | $389,000
(
    '7001003', 'Active', '2026-04-01', NULL, NULL, 8,
    389000, 399000, NULL,
    '100 3rd Ave S Unit 2805', 'Minneapolis', '55401', 'Hennepin', 'Mill District', 'Mill District Residences',
    'Condo', 'High Rise', 2002, 'One', 'Previously Owned',
    2, 2, 0, 0, 0,
    1380, 0, 1380, 1380,
    NULL, NULL, 1, NULL, 'None',
    '[{"room":"Living Room","level":"Main","dim":"15x20"},{"room":"Kitchen","level":"Main","dim":"10x14"},{"room":"Bedroom 1","level":"Main","dim":"13x15"},{"room":"Bedroom 2","level":"Main","dim":"11x13"},{"room":"Den","level":"Main","dim":"9x11"}]',
    'Dishwasher, Disposal, Dryer, Microwave, Range, Refrigerator, Washer',
    'None', 'Forced Air', 'Central', 'Natural Gas',
    NULL, 0,
    'Concrete, Block', 'Balcony',
    'Flat', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    NULL, NULL,
    'Eat In Kitchen', NULL,
    'Balcony, Ceiling Fan(s), City Views, Hardwood Floors, In-Unit Laundry, Panoramic View, Security System',
    'Assigned, Covered, Heated Garage, Underground', 'In-Unit', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    698, 'Monthly', 'Hazard Insurance, Lawn Care, Maintenance Grounds, Professional Mgmt, Trash, Water',
    'FirstService Residential', '612-555-0200',
    5840, 2026, '5840',
    'Whittier', 'Jefferson', 'Southwest', '001 - Minneapolis',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Spectacular 28th floor corner unit with floor-to-ceiling windows delivering sweeping views of the Mississippi River, Stone Arch Bridge, and the Minneapolis skyline. Two bedrooms, two full baths, den, and open living/dining with hardwood floors throughout. Updated kitchen with granite, stainless appliances, and breakfast bar. Primary suite with walk-in closet and en-suite bath. In-unit washer/dryer. One heated underground parking space included. Building amenities: 24-hour concierge, fitness center, rooftop deck, party room. Walk to Gold Medal Park, Guthrie Theater, US Bank Stadium, and the best restaurants in the Mill District.',
    38, 'Located on 3rd Ave S between 1st and 2nd St S in the Mill District. Guest parking on 3rd Ave.'
),

-- 4. Pending | Single Family | Eden Prairie | $625,000
(
    '7001004', 'Pending', '2026-03-05', '2026-04-02', NULL, 28,
    625000, 649000, NULL,
    '9217 Olympia Dr', 'Eden Prairie', '55347', 'Hennepin', NULL, 'Bearpath',
    'Single Family', 'Two Story', 1995, 'Two', 'Previously Owned',
    4, 2, 1, 1, 0,
    2400, 1050, 3450, 1200,
    18730, '115x163', 3, 682, 'None',
    '[{"room":"Living Room","level":"Main","dim":"14x17"},{"room":"Dining Room","level":"Main","dim":"12x14"},{"room":"Kitchen","level":"Main","dim":"14x16"},{"room":"Family Room","level":"Main","dim":"16x18"},{"room":"Bedroom 1","level":"Upper","dim":"14x16"},{"room":"Bedroom 2","level":"Upper","dim":"11x13"},{"room":"Bedroom 3","level":"Upper","dim":"11x12"},{"room":"Bedroom 4","level":"Upper","dim":"10x12"},{"room":"Recreation Room","level":"Lower","dim":"16x22"},{"room":"Office","level":"Lower","dim":"10x12"}]',
    'Air-To-Air Exchanger, Dishwasher, Disposal, Dryer, Furnace Humidifier, Microwave, Range, Refrigerator, Washer, Water Softener - Owned',
    'Drain Tiled, Finished (Livable), Full, Sump Pump',
    'Forced Air', 'Central', 'Natural Gas',
    'Gas Burning, Family Room', 1,
    'Brick/Stone, Vinyl', 'Deck',
    'Asphalt Shingles', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    NULL, 'Tree Coverage - Medium',
    'Informal Dining Room', 'Family Room, Main Level',
    'Ceiling Fan(s), Deck, Hardwood Floors, In-Ground Sprinkler, Kitchen Center Island, Primary Bedroom Walk-In Closet',
    'Attached Garage, Driveway - Asphalt', 'Laundry Room, Main Level', 'Cash, Conventional, FHA',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    8200, 2026, '8200',
    'Cedar Ridge', 'Southwest', 'Eden Prairie', '272 - Eden Prairie',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Well-maintained Bearpath-area two-story with four bedrooms and three baths on a private wooded lot. Updated kitchen with granite countertops, stainless appliances, and center island opens to family room with gas fireplace. Main level hardwood floors, formal living and dining, mudroom off three-car garage. Upper level primary suite with walk-in closet and private bath. Three additional bedrooms share a full bath. Finished lower level with recreation room, office, and rough-in for future bath. Deck overlooking private backyard. Eden Prairie schools. SALE PENDING — BACKUP OFFERS CONSIDERED.',
    35, 'From Hwy 212, north on Townline Rd, right on Olympia Dr, home on left.'
),

-- 5. Sold | Single Family | Minnetonka | $542,000
(
    '7001005', 'Sold', '2026-01-10', '2026-01-28', '2026-03-14', 18,
    549000, 549000, 542000,
    '4412 Shady Oak Rd', 'Minnetonka', '55343', 'Hennepin', NULL, NULL,
    'Single Family', 'Split Level', 1972, 'Multi/Split', 'Previously Owned',
    4, 1, 1, 1, 0,
    1620, 980, 2600, 1100,
    12197, '90x135', 2, 462, 'None',
    '[{"room":"Living Room","level":"Main","dim":"14x18"},{"room":"Kitchen","level":"Main","dim":"11x14"},{"room":"Dining Room","level":"Main","dim":"10x12"},{"room":"Bedroom 1","level":"Upper","dim":"13x14"},{"room":"Bedroom 2","level":"Upper","dim":"11x12"},{"room":"Bedroom 3","level":"Upper","dim":"10x11"},{"room":"Family Room","level":"Lower","dim":"14x20"},{"room":"Bedroom 4","level":"Lower","dim":"10x12"}]',
    'Dishwasher, Dryer, Microwave, Range, Refrigerator, Washer',
    'Block, Daylight/Lookout Windows, Finished (Livable), Partial',
    'Forced Air', 'Central', 'Natural Gas',
    'Wood Burning, Family Room', 1,
    'Brick/Stone, Vinyl', 'Deck',
    'Asphalt Shingles', 'Circuit Breakers', 'City Sewer/Connected', 'City Water/Connected',
    NULL, 'Tree Coverage - Light',
    'Eat In Kitchen', 'Family Room, Lower Level',
    'Ceiling Fan(s), Deck, Hardwood Floors',
    'Attached Garage, Driveway - Asphalt', 'In-Unit', 'Cash, Conventional, FHA',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    7100, 2025, '7100',
    'Scenic Heights', 'Minnetonka', 'Minnetonka', '276 - Minnetonka',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'SOLD — Classic Minnetonka split-level on a mature tree-lined lot. Updated kitchen and baths. Hardwood floors on main level. Lower level family room with wood-burning fireplace. Two-car attached garage. Convenient to Hwy 169 and shopping.',
    24, 'From Hwy 169, west on Shady Oak Rd, home on right.'
),

-- 6. Active | Townhouse | Plymouth | $415,000
(
    '7001006', 'Active', '2026-04-05', NULL, NULL, 4,
    415000, 415000, NULL,
    '3380 Vagabond Ln N', 'Plymouth', '55441', 'Hennepin', NULL, 'Kingsview',
    'Townhouse', 'Two Story', 2015, 'Two', 'Previously Owned',
    3, 2, 0, 1, 0,
    1580, 620, 2200, 800,
    3049, NULL, 2, 420, 'None',
    '[{"room":"Living Room","level":"Main","dim":"14x16"},{"room":"Kitchen","level":"Main","dim":"10x14"},{"room":"Dining Room","level":"Main","dim":"10x12"},{"room":"Bedroom 1","level":"Upper","dim":"13x15"},{"room":"Bedroom 2","level":"Upper","dim":"11x13"},{"room":"Bedroom 3","level":"Upper","dim":"10x11"},{"room":"Family Room","level":"Lower","dim":"13x16"}]',
    'Dishwasher, Disposal, Dryer, Microwave, Range, Refrigerator, Washer',
    'Finished (Livable), Full',
    'Forced Air', 'Central', 'Natural Gas',
    NULL, 0,
    'Brick/Stone, Vinyl', 'Patio',
    'Asphalt Shingles', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    NULL, NULL,
    'Eat In Kitchen', NULL,
    'Ceiling Fan(s), Hardwood Floors, Kitchen Center Island, Primary Bedroom Walk-In Closet, Washer/Dryer Hookup',
    'Attached Garage, Driveway - Asphalt', 'In-Unit, Upper Level', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    325, 'Monthly', 'Hazard Insurance, Lawn Care, Maintenance Grounds, Professional Mgmt, Snow Removal',
    'Associa', '763-555-0300',
    4980, 2026, '4980',
    'Sunset Hill', 'Wayzata', 'Wayzata', '284 - Wayzata',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Like-new end-unit townhome in popular Kingsview. Open main level with hardwood floors, kitchen island with quartz countertops, and stainless appliances. Main level half bath and mudroom off two-car garage. Upper level primary suite with walk-in closet and double-sink bath. Two additional bedrooms and full bath. Upper level laundry. Finished lower level family room with daylight windows. Association covers lawn care, snow removal, and exterior maintenance. Wayzata schools.',
    28, 'From I-494, north on Hwy 169, east on Medicine Lake Rd, north on Vagabond Ln, home on right.'
),

-- 7. Active | Single Family | Orono | Lakefront | $7,500,000
(
    '7001007', 'Active', '2026-02-01', NULL, NULL, 60,
    7500000, 7900000, NULL,
    '2180 Shadywood Rd', 'Orono', '55364', 'Hennepin', NULL, NULL,
    'Single Family', 'One Story', 2019, 'One', 'Previously Owned',
    5, 4, 2, 1, 0,
    5200, 3100, 8300, 5200,
    108900, '200x544x210x520', 4, 1600, 'Below Ground, Heated, Outdoor',
    '[{"room":"Great Room","level":"Main","dim":"26x32"},{"room":"Kitchen","level":"Main","dim":"20x24"},{"room":"Dining Room","level":"Main","dim":"16x20"},{"room":"Office","level":"Main","dim":"14x16"},{"room":"Bedroom 1","level":"Main","dim":"20x24"},{"room":"Bedroom 2","level":"Main","dim":"14x16"},{"room":"Bedroom 3","level":"Main","dim":"13x15"},{"room":"Bedroom 4","level":"Lower","dim":"14x16"},{"room":"Bedroom 5","level":"Lower","dim":"13x15"},{"room":"Recreation Room","level":"Lower","dim":"28x36"},{"room":"Exercise Room","level":"Lower","dim":"16x20"},{"room":"Wine Cellar","level":"Lower","dim":"10x14"}]',
    'Air-To-Air Exchanger, Central Vacuum, Cooktop, Dishwasher, Disposal, Dryer, Exhaust Fan/Hood, Freezer, Furnace Humidifier, Microwave, Refrigerator, Wall Oven, Washer, Water Filtration System, Water Softener - Owned, Wine Cooler, Double Oven, Stainless Steel Appliances',
    'Drain Tiled, Finished (Livable), Full, Storage Space, Walkout',
    'Forced Air, In-Floor Heating, Radiant', 'Central', 'Natural Gas',
    'Gas Burning, Great Room, Primary Bedroom', 4,
    'Cedar, Stone', 'Balcony, Deck, Dock, Outdoor Kitchen, Patio, Screened Porch',
    'Architectural Shingle, Metal', '200+ Amp Service', 'City Sewer/Connected', 'Well',
    NULL, 'Accessible Shoreline, Tree Coverage - Medium',
    'Informal Dining Room, Separate/Formal Dining Room', 'Great Room, Lower Level, Main Level',
    'Balcony, Boat Slip, Deck, Dock, Exercise Room, French Doors, Hardwood Floors, In-Ground Sprinkler, Kitchen Center Island, Natural Woodwork, Outdoor Kitchen, Panoramic View, Patio, Primary Bedroom Walk-In Closet, Sauna, Security System, Vaulted Ceiling(s), Wet Bar',
    'Attached Garage, Driveway - Asphalt, Heated Garage, Insulated Garage', 'Laundry Room, Main Level', 'Cash, Conventional',
    200, 'West, South, Lake', 'Lake Minnetonka', 'Lake',
    'General Development', '27013300', '14205.6', '113', 'Sand',
    NULL, NULL, NULL, NULL, NULL,
    64000, 2026, '64000',
    'Orono', 'Orono', 'Orono', '278 - Orono',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Extraordinary new construction on 200 feet of coveted Lake Minnetonka shoreline in Orono. Designed by award-winning architect with interiors by a nationally recognized design firm. Single-level main floor living with soaring ceilings, floor-to-ceiling windows, and seamless indoor-outdoor flow to the lake. Chef''s kitchen with custom cabinetry, professional appliances, and water views. Sprawling primary suite with dual walk-in closets, spa bath with heated floors, and screened porch overlooking the lake. Four additional bedrooms. Walkout lower level with recreation room, wine cellar, exercise room, and sauna. Heated four-car garage with epoxy floors. In-ground pool. Private dock with boat lift. Orono schools.',
    97, 'From Wayzata, west on Minnetonka Blvd, north on Shadywood Rd to address. Gate at driveway.'
),

-- 8. TNAS | Single Family | Minnetonka Beach | $1,450,000
(
    '7001008', 'TNAS', '2026-03-20', NULL, NULL, 17,
    1450000, 1450000, NULL,
    '4580 Vine Hill Rd', 'Minnetonka Beach', '55361', 'Hennepin', NULL, NULL,
    'Single Family', 'Two Story', 1991, 'Two', 'Previously Owned',
    5, 3, 1, 1, 0,
    3200, 1600, 4800, 1600,
    37897, '165x230', 3, 840, 'None',
    '[{"room":"Living Room","level":"Main","dim":"16x20"},{"room":"Dining Room","level":"Main","dim":"14x16"},{"room":"Kitchen","level":"Main","dim":"16x20"},{"room":"Family Room","level":"Main","dim":"18x22"},{"room":"Bedroom 1","level":"Upper","dim":"16x20"},{"room":"Bedroom 2","level":"Upper","dim":"12x14"},{"room":"Bedroom 3","level":"Upper","dim":"12x13"},{"room":"Bedroom 4","level":"Upper","dim":"11x12"},{"room":"Bedroom 5","level":"Lower","dim":"12x14"},{"room":"Recreation Room","level":"Lower","dim":"20x28"}]',
    'Air-To-Air Exchanger, Dishwasher, Disposal, Dryer, Microwave, Range, Refrigerator, Washer, Water Softener - Owned',
    'Drain Tiled, Finished (Livable), Full, Walkout',
    'Forced Air', 'Central', 'Natural Gas',
    'Gas Burning, Family Room, Primary Bedroom', 2,
    'Stucco', 'Deck, Patio',
    'Asphalt Shingles', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    'Partial', 'Tree Coverage - Heavy',
    'Separate/Formal Dining Room', 'Family Room, Main Level',
    'Ceiling Fan(s), Deck, Hardwood Floors, In-Ground Sprinkler, Kitchen Center Island, Patio, Primary Bedroom Walk-In Closet, Vaulted Ceiling(s)',
    'Attached Garage, Driveway - Asphalt', 'Laundry Room, Main Level', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    18200, 2026, '18200',
    'Minnetonka Beach', 'Wayzata', 'Wayzata', '284 - Wayzata',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'TEMPORARILY NOT AVAILABLE FOR SHOWING — Available again 05/01/2026. Contact listing agent for details. Stunning Minnetonka Beach two-story on nearly an acre with seasonal lake views. Updated kitchen and primary bath. Three-car garage. Finished walkout lower level. Orono/Wayzata school districts.',
    12, 'From Minnetonka Blvd, south on Vine Hill Rd, home on right.'
),

-- 9. Active | Single Family | Deephaven | $1,100,000
(
    '7001009', 'Active', '2026-03-28', NULL, NULL, 11,
    1100000, 1100000, NULL,
    '20240 Cottagewood Rd', 'Deephaven', '55331', 'Hennepin', 'Cottagewood', NULL,
    'Single Family', 'One Story', 1962, 'One', 'Previously Owned',
    4, 2, 1, 1, 0,
    2100, 1800, 3900, 2100,
    32670, '150x218', 2, 528, 'None',
    '[{"room":"Living Room","level":"Main","dim":"16x24"},{"room":"Kitchen","level":"Main","dim":"14x18"},{"room":"Dining Room","level":"Main","dim":"12x16"},{"room":"Bedroom 1","level":"Main","dim":"14x16"},{"room":"Bedroom 2","level":"Main","dim":"12x14"},{"room":"Bedroom 3","level":"Main","dim":"11x13"},{"room":"Bedroom 4","level":"Lower","dim":"13x15"},{"room":"Family Room","level":"Lower","dim":"18x26"},{"room":"Recreation Room","level":"Lower","dim":"14x20"}]',
    'Air-To-Air Exchanger, Cooktop, Dishwasher, Disposal, Dryer, Refrigerator, Wall Oven, Washer, Water Softener - Owned',
    'Drain Tiled, Finished (Livable), Full, Walkout',
    'Forced Air', 'Central', 'Natural Gas',
    'Gas Burning, Living Room', 1,
    'Brick/Stone, Cedar', 'Deck, Patio',
    'Asphalt Shingles', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    'None', 'Tree Coverage - Heavy',
    'Separate/Formal Dining Room', 'Family Room, Lower Level',
    'Ceiling Fan(s), Deck, Hardwood Floors, Kitchen Center Island, Patio, Panoramic View',
    'Attached Garage, Driveway - Asphalt', 'Laundry Room, Lower Level', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    14800, 2026, '14800',
    'Cottagewood', 'Minnetonka', 'Minnetonka', '276 - Minnetonka',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Rare rambler on a stunning .75-acre wooded lot in the heart of Deephaven''s sought-after Cottagewood neighborhood. Thoughtfully updated main level with hardwood floors, chef''s kitchen, and a generous living room with fireplace and vaulted wood ceiling. All three main level bedrooms have been updated. Walkout lower level features a fourth bedroom, full bath, family room with wet bar, and recreation room with walk-out patio. Steps from Cottagewood Sailing Club and Lake Minnetonka regional trail.',
    44, 'From Minnetonka Blvd, south on Vine Hill, right on Cottagewood Rd, home on right.'
),

-- 10. Active | Single Family | St. Louis Park | $499,000
(
    '7001010', 'Active', '2026-04-07', NULL, NULL, 2,
    499000, 499000, NULL,
    '2744 Inglewood Ave S', 'St. Louis Park', '55416', 'Hennepin', 'Minikahda Vista', NULL,
    'Single Family', 'One Story', 1955, 'One', 'Previously Owned',
    3, 1, 1, 0, 0,
    1350, 1100, 2450, 1350,
    8712, '66x132', 2, 440, 'None',
    '[{"room":"Living Room","level":"Main","dim":"13x18"},{"room":"Kitchen","level":"Main","dim":"10x14"},{"room":"Dining Room","level":"Main","dim":"10x12"},{"room":"Bedroom 1","level":"Main","dim":"12x14"},{"room":"Bedroom 2","level":"Main","dim":"10x12"},{"room":"Bedroom 3","level":"Main","dim":"10x10"},{"room":"Family Room","level":"Lower","dim":"13x22"},{"room":"Office","level":"Lower","dim":"9x11"}]',
    'Dishwasher, Dryer, Microwave, Range, Refrigerator, Washer',
    'Finished (Livable), Full',
    'Forced Air', 'Central', 'Natural Gas',
    'Wood Burning, Living Room', 1,
    'Stucco', 'Patio',
    'Asphalt Shingles', 'Circuit Breakers', 'City Sewer/Connected', 'City Water/Connected',
    'Full', 'Tree Coverage - Light',
    'Eat In Kitchen', 'Family Room, Lower Level',
    'Ceiling Fan(s), Hardwood Floors',
    'Detached Garage, Driveway - Asphalt', 'In-Unit', 'Cash, Conventional, FHA',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    6400, 2026, '6400',
    'Aquila', 'St. Louis Park', 'St. Louis Park', '283 - St. Louis Park',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Charming Minikahda Vista rambler updated throughout. Hardwood floors, updated kitchen with new appliances, and a light-filled living room with wood-burning fireplace. Three main-level bedrooms and updated full bath. Finished basement with family room, office, laundry, and rough-in bath. Two-car detached garage. Fully fenced yard with patio. Steps from Minnehaha Creek trails. St. Louis Park schools. Quick close possible.',
    31, 'From Excelsior Blvd, north on Inglewood Ave S, home on right between 27th and 28th.'
),

-- 11. Sold | Condo | Edina | $268,000
(
    '7001011', 'Sold', '2026-01-20', '2026-02-05', '2026-03-07', 16,
    275000, 285000, 268000,
    '3901 W 50th St Unit 101', 'Edina', '55424', 'Hennepin', '50th and France', 'Edina Towers',
    'Condo', 'Low Rise', 1968, 'One', 'Previously Owned',
    2, 1, 0, 0, 0,
    1020, 0, 1020, 1020,
    NULL, NULL, 1, NULL, 'None',
    '[{"room":"Living Room","level":"Main","dim":"13x16"},{"room":"Kitchen","level":"Main","dim":"8x12"},{"room":"Dining Room","level":"Main","dim":"9x11"},{"room":"Bedroom 1","level":"Main","dim":"12x14"},{"room":"Bedroom 2","level":"Main","dim":"10x12"}]',
    'Dishwasher, Disposal, Microwave, Range, Refrigerator',
    'None', 'Forced Air', 'Central', 'Natural Gas',
    NULL, 0,
    'Brick/Stone', NULL,
    'Flat', 'Circuit Breakers', 'City Sewer/Connected', 'City Water/Connected',
    NULL, NULL,
    'Eat In Kitchen', NULL,
    'Hardwood Floors',
    'Assigned, Covered', 'Common Area', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    598, 'Monthly', 'Hazard Insurance, Heat, Lawn Care, Maintenance Grounds, Professional Mgmt, Trash, Water',
    'Edina Realty Property Mgmt', '952-555-0400',
    3200, 2025, '3200',
    'Cornelia', 'Valley View', 'Edina', '273 - Edina',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'SOLD — Ground floor two-bedroom condo steps from 50th and France shopping and dining. Updated kitchen and hardwood floors. One covered parking space. Association covers heat, water, and exterior. Edina schools.',
    18, 'On 50th St between France and Wooddale in the Edina Towers building.'
),

-- 12. Active | Single Family | Excelsior | $725,000
(
    '7001012', 'Active', '2026-03-10', NULL, NULL, 29,
    725000, 749000, NULL,
    '331 Oak St', 'Excelsior', '55331', 'Hennepin', 'Downtown Excelsior', NULL,
    'Single Family', 'Two Story', 1912, 'Two', 'Previously Owned',
    4, 2, 0, 1, 0,
    2050, 600, 2650, 1050,
    10454, '80x130', 2, 480, 'None',
    '[{"room":"Living Room","level":"Main","dim":"14x18"},{"room":"Dining Room","level":"Main","dim":"12x15"},{"room":"Kitchen","level":"Main","dim":"12x16"},{"room":"Den","level":"Main","dim":"10x12"},{"room":"Bedroom 1","level":"Upper","dim":"14x16"},{"room":"Bedroom 2","level":"Upper","dim":"12x14"},{"room":"Bedroom 3","level":"Upper","dim":"11x12"},{"room":"Bedroom 4","level":"Upper","dim":"10x11"}]',
    'Dishwasher, Dryer, Range, Refrigerator, Washer',
    'Partial, Stone, Unfinished',
    'Forced Air, Radiator(s)', 'Window Unit(s)', 'Natural Gas',
    'Wood Burning, Living Room', 1,
    'Cedar', 'Porch',
    'Asphalt Shingles', 'Circuit Breakers', 'City Sewer/Connected', 'City Water/Connected',
    'Full', 'Tree Coverage - Medium',
    'Separate/Formal Dining Room', NULL,
    'Ceiling Fan(s), French Doors, Hardwood Floors, Natural Woodwork, Paneled Doors, Porch',
    'Detached Garage, Driveway - Asphalt', 'In-Unit, Lower Level', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    9200, 2026, '9200',
    'Excelsior', 'Minnetonka', 'Minnetonka', '276 - Minnetonka',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Historic 1912 Arts and Crafts home two blocks from downtown Excelsior and steps from Lake Minnetonka. Gorgeous original woodwork, hardwood floors, and built-ins throughout. Updated kitchen with soapstone countertops and farmhouse sink. Four bedrooms on the upper level. Wrap-around porch, fully fenced yard, two-car detached garage. Walk to the lake, restaurants, shops, and the Excelsior Streetcar Trolley.',
    33, 'From Hwy 7, south on Water St into downtown Excelsior, right on Oak St.'
),

-- 13. Pending | Single Family | Minnetonka | $559,000
(
    '7001013', 'Pending', '2026-03-18', '2026-04-06', NULL, 19,
    559000, 579000, NULL,
    '6015 Glenwood Rd', 'Minnetonka', '55345', 'Hennepin', NULL, NULL,
    'Single Family', 'Split Level', 1978, 'Multi/Split', 'Previously Owned',
    4, 1, 1, 1, 0,
    1740, 860, 2600, 1100,
    14810, '100x148', 2, 484, 'None',
    '[{"room":"Living Room","level":"Main","dim":"13x18"},{"room":"Kitchen","level":"Main","dim":"11x14"},{"room":"Dining Room","level":"Main","dim":"10x13"},{"room":"Bedroom 1","level":"Upper","dim":"13x14"},{"room":"Bedroom 2","level":"Upper","dim":"10x12"},{"room":"Bedroom 3","level":"Upper","dim":"10x11"},{"room":"Family Room","level":"Lower","dim":"14x18"},{"room":"Bedroom 4","level":"Lower","dim":"10x12"}]',
    'Dishwasher, Dryer, Furnace Humidifier, Microwave, Range, Refrigerator, Washer',
    'Block, Daylight/Lookout Windows, Finished (Livable), Partial',
    'Forced Air', 'Central', 'Natural Gas',
    'Gas Burning, Family Room', 1,
    'Brick/Stone, Vinyl', 'Deck',
    'Asphalt Shingles', 'Circuit Breakers', 'City Sewer/Connected', 'City Water/Connected',
    NULL, 'Tree Coverage - Medium',
    'Eat In Kitchen', 'Family Room, Lower Level',
    'Ceiling Fan(s), Deck, Hardwood Floors',
    'Attached Garage, Driveway - Asphalt', 'In-Unit, Lower Level', 'Cash, Conventional, FHA',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    7400, 2026, '7400',
    'Scenic Heights', 'Minnetonka', 'Minnetonka', '276 - Minnetonka',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'SALE PENDING. Well-kept Minnetonka split-level on a mature corner lot. Updated kitchen and bathrooms. Hardwood floors on main level. Lower level family room with gas fireplace, fourth bedroom, and laundry. Two-car attached garage. Large deck and yard.',
    27, 'From Hwy 169, west on Glenwood Rd, home at corner of Glenwood and Blake Rd.'
),

-- 14. Active | Single Family | Long Lake | $850,000
(
    '7001014', 'Active', '2026-03-22', NULL, NULL, 15,
    850000, 850000, NULL,
    '1440 Willow Dr', 'Long Lake', '55356', 'Hennepin', NULL, NULL,
    'Single Family', 'Two Story', 2001, 'Two', 'Previously Owned',
    5, 3, 0, 1, 0,
    2700, 1350, 4050, 1350,
    30056, '140x215', 3, 756, 'None',
    '[{"room":"Great Room","level":"Main","dim":"18x22"},{"room":"Kitchen","level":"Main","dim":"14x18"},{"room":"Dining Room","level":"Main","dim":"12x15"},{"room":"Office","level":"Main","dim":"11x13"},{"room":"Bedroom 1","level":"Upper","dim":"15x18"},{"room":"Bedroom 2","level":"Upper","dim":"12x14"},{"room":"Bedroom 3","level":"Upper","dim":"12x13"},{"room":"Bedroom 4","level":"Upper","dim":"11x12"},{"room":"Bedroom 5","level":"Lower","dim":"12x14"},{"room":"Recreation Room","level":"Lower","dim":"18x24"}]',
    'Air-To-Air Exchanger, Cooktop, Dishwasher, Disposal, Dryer, Exhaust Fan/Hood, Microwave, Refrigerator, Wall Oven, Washer, Water Softener - Owned',
    'Drain Tiled, Finished (Livable), Full, Sump Pump',
    'Forced Air', 'Central', 'Natural Gas',
    'Gas Burning, Great Room, Primary Bedroom', 2,
    'Brick/Stone, Engineered Wood', 'Deck, Patio',
    'Asphalt Shingles', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    'Partial', 'Tree Coverage - Medium',
    'Informal Dining Room', 'Great Room, Main Level',
    'Ceiling Fan(s), Deck, Hardwood Floors, In-Ground Sprinkler, Kitchen Center Island, Patio, Primary Bedroom Walk-In Closet, Vaulted Ceiling(s)',
    'Attached Garage, Driveway - Asphalt, Heated Garage', 'Laundry Room, Main Level', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    10800, 2026, '10800',
    'Long Lake', 'Wayzata', 'Wayzata', '284 - Wayzata',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Exceptional Long Lake two-story on .69 acres with mature trees and exceptional privacy. Two-story great room with stone fireplace, hardwood floors, and updated kitchen with center island and professional appliances. Main level office, formal dining, and mudroom off heated three-car garage. Upper level primary suite with fireplace, walk-in closet, and spa bath. Four additional bedrooms. Finished lower level with rec room, fifth bedroom, bath, and storage. Wayzata schools.',
    48, 'From Hwy 12, north on Co Rd 6, east on Willow Dr, home on right.'
),

-- 15. Active | Single Family | Hopkins | $349,000
(
    '7001015', 'Active', '2026-04-04', NULL, NULL, 5,
    349000, 349000, NULL,
    '15 Jackson Ave N', 'Hopkins', '55343', 'Hennepin', 'East Hopkins', NULL,
    'Single Family', 'One Story', 1949, 'One', 'Previously Owned',
    3, 1, 0, 0, 0,
    1100, 800, 1900, 1100,
    6534, '52x126', 1, 240, 'None',
    '[{"room":"Living Room","level":"Main","dim":"13x16"},{"room":"Kitchen","level":"Main","dim":"10x12"},{"room":"Bedroom 1","level":"Main","dim":"11x13"},{"room":"Bedroom 2","level":"Main","dim":"10x11"},{"room":"Bedroom 3","level":"Main","dim":"9x10"},{"room":"Family Room","level":"Lower","dim":"13x20"}]',
    'Dishwasher, Dryer, Range, Refrigerator, Washer',
    'Finished (Livable), Full',
    'Forced Air', 'Central', 'Natural Gas',
    NULL, 0,
    'Vinyl', 'None',
    'Asphalt Shingles', 'Circuit Breakers', 'City Sewer/Connected', 'City Water/Connected',
    NULL, 'Tree Coverage - Light',
    'Eat In Kitchen', 'Family Room, Lower Level',
    'Hardwood Floors',
    'Detached Garage, Driveway - Asphalt', 'In-Unit, Lower Level', 'Cash, Conventional, FHA, VA',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    4100, 2026, '4100',
    'Hopkins', 'Eisenhower', 'Hopkins', '270 - Hopkins',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Move-in ready Hopkins rambler with hardwood floors and an updated kitchen. Three main-level bedrooms and full bath. Finished basement family room, laundry, and storage. One-car detached garage. Fenced yard. Quick access to Hwy 169, light rail, and downtown Hopkins dining.',
    22, 'From Hwy 169, east on Excelsior Blvd, north on Jackson Ave N, home on right.'
),

-- 16. Sold | Single Family | Wayzata | $2,100,000
(
    '7001016', 'Sold', '2025-12-01', '2025-12-18', '2026-02-14', 17,
    2195000, 2195000, 2100000,
    '415 Bushaway Rd', 'Wayzata', '55391', 'Hennepin', NULL, NULL,
    'Single Family', 'Two Story', 2008, 'Two', 'Previously Owned',
    5, 3, 1, 1, 0,
    3800, 1900, 5700, 1900,
    47916, '180x266', 3, 900, 'None',
    '[{"room":"Great Room","level":"Main","dim":"20x26"},{"room":"Kitchen","level":"Main","dim":"16x20"},{"room":"Dining Room","level":"Main","dim":"14x16"},{"room":"Study","level":"Main","dim":"12x14"},{"room":"Bedroom 1","level":"Upper","dim":"16x20"},{"room":"Bedroom 2","level":"Upper","dim":"13x15"},{"room":"Bedroom 3","level":"Upper","dim":"12x14"},{"room":"Bedroom 4","level":"Upper","dim":"12x13"},{"room":"Bedroom 5","level":"Lower","dim":"13x15"},{"room":"Recreation Room","level":"Lower","dim":"22x28"}]',
    'Air-To-Air Exchanger, Central Vacuum, Cooktop, Dishwasher, Disposal, Dryer, Exhaust Fan/Hood, Furnace Humidifier, Refrigerator, Wall Oven, Washer, Water Softener - Owned',
    'Drain Tiled, Finished (Livable), Full, Walkout',
    'Forced Air, In-Floor Heating', 'Central', 'Natural Gas',
    'Gas Burning, Great Room, Primary Bedroom', 3,
    'Stucco, Stone', 'Deck, Patio, Screened Porch',
    'Architectural Shingle', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    NULL, 'Tree Coverage - Medium',
    'Informal Dining Room, Separate/Formal Dining Room', 'Great Room, Lower Level',
    'Ceiling Fan(s), Deck, Hardwood Floors, In-Ground Sprinkler, Kitchen Center Island, Primary Bedroom Walk-In Closet, Screened Porch, Security System, Vaulted Ceiling(s)',
    'Attached Garage, Driveway - Asphalt, Heated Garage', 'Laundry Room, Main Level', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    24000, 2025, '24000',
    'Wayzata', 'Wayzata', 'Wayzata', '284 - Wayzata',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'SOLD $2,100,000. Exceptional Wayzata two-story on 1.1 acres with three-car heated garage. Gourmet kitchen with Wolf range and Sub-Zero refrigerator. Three fireplaces. Screened porch. Walkout lower level.',
    22, 'From Wayzata Blvd, south on Bushaway Rd.'
),

-- 17. Active | Single Family | Shorewood | $599,000
(
    '7001017', 'Active', '2026-04-02', NULL, NULL, 7,
    599000, 599000, NULL,
    '24880 Forest Blvd', 'Shorewood', '55331', 'Hennepin', NULL, NULL,
    'Single Family', 'Two Story', 1989, 'Two', 'Previously Owned',
    4, 2, 1, 1, 0,
    2200, 900, 3100, 1100,
    21780, '120x182', 2, 528, 'None',
    '[{"room":"Living Room","level":"Main","dim":"14x18"},{"room":"Dining Room","level":"Main","dim":"12x14"},{"room":"Kitchen","level":"Main","dim":"13x16"},{"room":"Family Room","level":"Main","dim":"16x18"},{"room":"Bedroom 1","level":"Upper","dim":"14x16"},{"room":"Bedroom 2","level":"Upper","dim":"11x13"},{"room":"Bedroom 3","level":"Upper","dim":"11x12"},{"room":"Bedroom 4","level":"Upper","dim":"10x12"},{"room":"Recreation Room","level":"Lower","dim":"16x20"}]',
    'Air-To-Air Exchanger, Dishwasher, Disposal, Dryer, Furnace Humidifier, Microwave, Range, Refrigerator, Washer, Water Softener - Owned',
    'Drain Tiled, Finished (Livable), Full, Sump Pump',
    'Forced Air', 'Central', 'Natural Gas',
    'Gas Burning, Family Room', 1,
    'Brick/Stone, Cedar', 'Deck',
    'Asphalt Shingles', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    NULL, 'Tree Coverage - Medium',
    'Separate/Formal Dining Room', 'Family Room, Main Level',
    'Ceiling Fan(s), Deck, Hardwood Floors, Kitchen Center Island, Primary Bedroom Walk-In Closet',
    'Attached Garage, Driveway - Asphalt', 'Laundry Room, Lower Level', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    8600, 2026, '8600',
    'Minnetonka', 'Minnetonka', 'Minnetonka', '276 - Minnetonka',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Solid Shorewood two-story on a half-acre wooded lot. Updated kitchen with granite and stainless. Hardwood floors. Main level family room with gas fireplace. Four upper bedrooms including primary with walk-in closet. Finished lower level recreation room. Two-car attached garage. Minnetonka schools.',
    32, 'From Hwy 7, south on Co Rd 19 to Forest Blvd, west to address.'
),

-- 18. Active | Single Family | Orono | $1,795,000
(
    '7001018', 'Active', '2026-03-01', NULL, NULL, 38,
    1795000, 1895000, NULL,
    '3005 Orono Orchard Rd', 'Orono', '55356', 'Hennepin', NULL, NULL,
    'Single Family', 'One Story', 2014, 'One', 'Previously Owned',
    4, 3, 1, 0, 0,
    3600, 2400, 6000, 3600,
    87120, '2 Acres', 4, 1200, 'Below Ground, Heated, Outdoor',
    '[{"room":"Great Room","level":"Main","dim":"22x28"},{"room":"Kitchen","level":"Main","dim":"18x20"},{"room":"Dining Room","level":"Main","dim":"14x16"},{"room":"Office","level":"Main","dim":"12x14"},{"room":"Bedroom 1","level":"Main","dim":"18x22"},{"room":"Bedroom 2","level":"Main","dim":"13x15"},{"room":"Bedroom 3","level":"Lower","dim":"14x16"},{"room":"Bedroom 4","level":"Lower","dim":"13x15"},{"room":"Recreation Room","level":"Lower","dim":"24x32"},{"room":"Exercise Room","level":"Lower","dim":"14x16"}]',
    'Air-To-Air Exchanger, Central Vacuum, Cooktop, Dishwasher, Disposal, Dryer, Exhaust Fan/Hood, Furnace Humidifier, Refrigerator, Wall Oven, Washer, Water Filtration System, Water Softener - Owned',
    'Drain Tiled, Finished (Livable), Full, Storage Space, Walkout',
    'Forced Air, In-Floor Heating, Radiant', 'Central', 'Natural Gas',
    'Gas Burning, Great Room, Primary Bedroom', 2,
    'Cedar, Stone', 'Deck, Patio, Screened Porch',
    'Architectural Shingle', '200+ Amp Service', 'City Sewer/Connected', 'Well',
    'Partial', 'Tree Coverage - Heavy',
    'Informal Dining Room', 'Great Room, Lower Level, Main Level',
    'Ceiling Fan(s), Deck, Exercise Room, Hardwood Floors, In-Ground Sprinkler, Kitchen Center Island, Patio, Primary Bedroom Walk-In Closet, Screened Porch, Security System, Vaulted Ceiling(s)',
    'Attached Garage, Driveway - Asphalt, Heated Garage, Insulated Garage', 'Laundry Room, Main Level', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    22000, 2026, '22000',
    'Orono', 'Orono', 'Orono', '278 - Orono',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Stunning custom rambler on 2 private acres in Orono. Single-level living with great room, gourmet kitchen, and main-level primary suite. In-ground heated pool. Walkout lower level. Four-car heated garage. Orono schools.',
    55, 'From Co Rd 6, north on Orono Orchard Rd, home at end of private drive.'
),

-- 19. Active | Townhouse | Minnetonka | $329,000
(
    '7001019', 'Active', '2026-04-06', NULL, NULL, 3,
    329000, 329000, NULL,
    '14820 Ironwood Ct', 'Minnetonka', '55345', 'Hennepin', NULL, 'Ironwood',
    'Townhouse', 'Two Story', 2003, 'Two', 'Previously Owned',
    2, 1, 1, 0, 0,
    1240, 0, 1240, 620,
    1742, NULL, 2, 420, 'None',
    '[{"room":"Living Room","level":"Main","dim":"12x16"},{"room":"Kitchen","level":"Main","dim":"9x12"},{"room":"Dining Room","level":"Main","dim":"9x10"},{"room":"Bedroom 1","level":"Upper","dim":"12x14"},{"room":"Bedroom 2","level":"Upper","dim":"10x12"},{"room":"Loft","level":"Upper","dim":"8x10"}]',
    'Dishwasher, Disposal, Dryer, Microwave, Range, Refrigerator, Washer',
    'None', 'Forced Air', 'Central', 'Natural Gas',
    NULL, 0,
    'Vinyl', 'Patio',
    'Asphalt Shingles', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    NULL, NULL,
    'Eat In Kitchen', NULL,
    'Ceiling Fan(s), Vaulted Ceiling(s), Walk-In Closet',
    'Attached Garage, Driveway - Asphalt', 'In-Unit, Upper Level', 'Cash, Conventional, FHA',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    268, 'Monthly', 'Hazard Insurance, Lawn Care, Maintenance Grounds, Professional Mgmt, Snow Removal, Trash',
    'Cities Management', '952-555-0500',
    3900, 2026, '3900',
    'Scenic Heights', 'Minnetonka', 'Minnetonka', '276 - Minnetonka',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Move-in ready two-bedroom townhome in a quiet Minnetonka cul-de-sac. Open main level with vaulted living room, kitchen with breakfast bar, and patio. Two upper-level bedrooms including primary with walk-in closet. Upper level loft. In-unit laundry. Two-car attached garage. Association covers lawn and snow. Minnetonka schools.',
    24, 'From Hwy 169, west on Co Rd 62, south on Bren Rd E, right on Ironwood Ct.'
),

-- 20. Active | Single Family | Victoria | $685,000
(
    '7001020', 'Active', '2026-03-25', NULL, NULL, 14,
    685000, 685000, NULL,
    '8455 Deer Run Dr', 'Victoria', '55386', 'Carver', NULL, 'Deer Run',
    'Single Family', 'Two Story', 2006, 'Two', 'Previously Owned',
    4, 2, 1, 1, 0,
    2450, 1100, 3550, 1250,
    19602, '120x163', 3, 682, 'None',
    '[{"room":"Living Room","level":"Main","dim":"14x16"},{"room":"Dining Room","level":"Main","dim":"12x14"},{"room":"Kitchen","level":"Main","dim":"13x16"},{"room":"Family Room","level":"Main","dim":"16x20"},{"room":"Bedroom 1","level":"Upper","dim":"14x16"},{"room":"Bedroom 2","level":"Upper","dim":"11x13"},{"room":"Bedroom 3","level":"Upper","dim":"11x12"},{"room":"Bedroom 4","level":"Upper","dim":"10x12"},{"room":"Recreation Room","level":"Lower","dim":"16x22"},{"room":"Office","level":"Lower","dim":"10x12"}]',
    'Air-To-Air Exchanger, Dishwasher, Disposal, Dryer, Furnace Humidifier, Microwave, Range, Refrigerator, Washer, Water Softener - Owned',
    'Drain Tiled, Finished (Livable), Full, Sump Pump',
    'Forced Air', 'Central', 'Natural Gas',
    'Gas Burning, Family Room', 1,
    'Brick/Stone, Vinyl', 'Deck',
    'Asphalt Shingles', '200+ Amp Service', 'City Sewer/Connected', 'City Water/Connected',
    NULL, 'Tree Coverage - Light',
    'Informal Dining Room', 'Family Room, Main Level',
    'Ceiling Fan(s), Deck, Hardwood Floors, In-Ground Sprinkler, Kitchen Center Island, Primary Bedroom Walk-In Closet',
    'Attached Garage, Driveway - Asphalt', 'Laundry Room, Main Level', 'Cash, Conventional',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL, NULL,
    8900, 2026, '8900',
    'Victoria', 'Chaska', 'Chaska', '112 - Eastern Carver County',
    'James Sawicki', '612-555-0100', '502000001',
    'Coldwell Banker Realty', '952-473-3000', '5141',
    'Beautiful Deer Run two-story on a private lot backing to trees. Updated kitchen with granite countertops, center island, and stainless appliances. Hardwood floors on main level. Family room with gas fireplace. Four upper bedrooms. Primary suite with walk-in closet and private bath. Finished lower level with recreation room, office, and rough-in bath. Three-car garage. In-ground sprinkler. Eastern Carver County schools.',
    41, 'From Hwy 5, south on Deer Run Dr into Deer Run neighborhood.'
);

UPDATE listings SET latitude = 44.9018, longitude = -93.3847 WHERE mls_id = '7001001';
UPDATE listings SET latitude = 44.9300, longitude = -93.5210 WHERE mls_id = '7001002';
UPDATE listings SET latitude = 44.9792, longitude = -93.2568 WHERE mls_id = '7001003';
UPDATE listings SET latitude = 44.8445, longitude = -93.4580 WHERE mls_id = '7001004';
UPDATE listings SET latitude = 44.9210, longitude = -93.4215 WHERE mls_id = '7001005';
UPDATE listings SET latitude = 45.0215, longitude = -93.4310 WHERE mls_id = '7001006';
UPDATE listings SET latitude = 44.9873, longitude = -93.6234 WHERE mls_id = '7001007';
UPDATE listings SET latitude = 44.9548, longitude = -93.5720 WHERE mls_id = '7001008';
UPDATE listings SET latitude = 44.9210, longitude = -93.5558 WHERE mls_id = '7001009';
UPDATE listings SET latitude = 44.9278, longitude = -93.3590 WHERE mls_id = '7001010';
UPDATE listings SET latitude = 44.9070, longitude = -93.3420 WHERE mls_id = '7001011';
UPDATE listings SET latitude = 44.9025, longitude = -93.5640 WHERE mls_id = '7001012';
UPDATE listings SET latitude = 44.9378, longitude = -93.4758 WHERE mls_id = '7001013';
UPDATE listings SET latitude = 44.9865, longitude = -93.5845 WHERE mls_id = '7001014';
UPDATE listings SET latitude = 44.9248, longitude = -93.4115 WHERE mls_id = '7001015';
UPDATE listings SET latitude = 44.9748, longitude = -93.5195 WHERE mls_id = '7001016';
UPDATE listings SET latitude = 44.8978, longitude = -93.5535 WHERE mls_id = '7001017';
UPDATE listings SET latitude = 44.9820, longitude = -93.6275 WHERE mls_id = '7001018';
UPDATE listings SET latitude = 44.9280, longitude = -93.4750 WHERE mls_id = '7001019';
UPDATE listings SET latitude = 44.8580, longitude = -93.6510 WHERE mls_id = '7001020';


-- ─────────────────────────────────────────────────────────────────────────────
-- IDX COMPLIANCE TEST DATA — synthetic brokerage diversity (added June 2026)
-- ─────────────────────────────────────────────────────────────────────────────
-- The original 20 INSERTs all assigned list_office_name = 'Coldwell Banker
-- Realty' and list_agent_name = 'James Sawicki', which made every synthetic
-- listing resolve to isOwnListing() === true on the Astro side. That left
-- the entire third-party / IDX-attribution code path unexercised in dev.
--
-- This block reassigns 12 of 20 listings to real Twin Cities competitor
-- brokerages while preserving every other field (address, coords, rooms,
-- amenities, etc.). Agent names and phone numbers are FICTITIOUS but
-- structurally plausible — do not contact, do not publish.
--
-- Final split:
--   8 listings → Coldwell Banker Realty / James Sawicki (own — UNCHANGED)
--                7001001, 7001004, 7001009, 7001012, 7001014,
--                7001017, 7001019, 7001020
--   3 listings → Edina Realty, Inc.            (7001002, 7001006, 7001010)
--   3 listings → RE/MAX Results                (7001003, 7001011, 7001015)
--   2 listings → Coldwell Banker Burnet        (7001005, 7001013)
--                CRITICAL TEST: shares "Coldwell Banker" prefix with our
--                brokerage. isOwnListing() must classify these as third-party
--                (false). Verifies the substring match doesn't false-positive.
--   2 listings → Keller Williams Premier Realty (7001008, 7001018)
--   2 listings → Lakes Sotheby's International  (7001007, 7001016)
-- ─────────────────────────────────────────────────────────────────────────────

-- Edina Realty, Inc.
UPDATE listings SET
    list_agent_name = 'Patricia Nelson',
    list_agent_phone = '952-555-2010',
    list_agent_mls_id = '502100201',
    list_office_name = 'Edina Realty, Inc.',
    list_office_phone = '952-925-8400',
    list_office_mls_id = '1101'
  WHERE mls_id IN ('7001002', '7001010');

UPDATE listings SET
    list_agent_name = 'David Anderson',
    list_agent_phone = '952-555-2015',
    list_agent_mls_id = '502100202',
    list_office_name = 'Edina Realty, Inc.',
    list_office_phone = '952-925-8400',
    list_office_mls_id = '1101'
  WHERE mls_id = '7001006';

-- RE/MAX Results
UPDATE listings SET
    list_agent_name = 'Maria Hernandez',
    list_agent_phone = '612-555-3020',
    list_agent_mls_id = '502100302',
    list_office_name = 'RE/MAX Results',
    list_office_phone = '612-928-3232',
    list_office_mls_id = '1201'
  WHERE mls_id IN ('7001003', '7001011');

UPDATE listings SET
    list_agent_name = 'Robert Schultz',
    list_agent_phone = '612-555-3025',
    list_agent_mls_id = '502100303',
    list_office_name = 'RE/MAX Results',
    list_office_phone = '612-928-3232',
    list_office_mls_id = '1201'
  WHERE mls_id = '7001015';

-- Coldwell Banker Burnet (NOT Coldwell Banker Realty — verifies substring
-- match in isOwnListing() doesn't false-positive on the shared "Coldwell
-- Banker" prefix)
UPDATE listings SET
    list_agent_name = 'Karen Lindstrom',
    list_agent_phone = '952-555-4040',
    list_agent_mls_id = '502100404',
    list_office_name = 'Coldwell Banker Burnet',
    list_office_phone = '952-475-3000',
    list_office_mls_id = '1305'
  WHERE mls_id IN ('7001005', '7001013');

-- Keller Williams Premier Realty
UPDATE listings SET
    list_agent_name = 'Daniel O''Brien',
    list_agent_phone = '612-555-5050',
    list_agent_mls_id = '502100505',
    list_office_name = 'Keller Williams Premier Realty',
    list_office_phone = '612-925-1100',
    list_office_mls_id = '1421'
  WHERE mls_id IN ('7001008', '7001018');

-- Lakes Sotheby's International Realty
UPDATE listings SET
    list_agent_name = 'Margaret Whitfield',
    list_agent_phone = '952-555-6060',
    list_agent_mls_id = '502100606',
    list_office_name = 'Lakes Sotheby''s International Realty',
    list_office_phone = '952-473-7000',
    list_office_mls_id = '1583'
  WHERE mls_id IN ('7001007', '7001016');


-- ─────────────────────────────────────────────────────────────────────────────
-- MODIFICATION TIMESTAMP — populates the "Data Updated" line on the IDX
-- disclosure block. Varied across recent/older to exercise display states.
-- Today's date for context: 2026-06-07.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE listings SET modification_timestamp = '2026-06-07 09:15:00' WHERE mls_id = '7001001';
UPDATE listings SET modification_timestamp = '2026-06-06 14:30:00' WHERE mls_id = '7001002';
UPDATE listings SET modification_timestamp = '2026-06-07 11:45:00' WHERE mls_id = '7001003';
UPDATE listings SET modification_timestamp = '2026-06-05 08:00:00' WHERE mls_id = '7001004';
UPDATE listings SET modification_timestamp = '2026-06-01 16:20:00' WHERE mls_id = '7001005';
UPDATE listings SET modification_timestamp = '2026-06-04 10:00:00' WHERE mls_id = '7001006';
UPDATE listings SET modification_timestamp = '2026-06-03 13:30:00' WHERE mls_id = '7001007';
UPDATE listings SET modification_timestamp = '2026-05-28 09:00:00' WHERE mls_id = '7001008';
UPDATE listings SET modification_timestamp = '2026-06-07 07:30:00' WHERE mls_id = '7001009';
UPDATE listings SET modification_timestamp = '2026-06-06 22:00:00' WHERE mls_id = '7001010';
UPDATE listings SET modification_timestamp = '2026-05-20 14:00:00' WHERE mls_id = '7001011';
UPDATE listings SET modification_timestamp = '2026-06-05 11:30:00' WHERE mls_id = '7001012';
UPDATE listings SET modification_timestamp = '2026-06-02 09:15:00' WHERE mls_id = '7001013';
UPDATE listings SET modification_timestamp = '2026-06-04 16:45:00' WHERE mls_id = '7001014';
UPDATE listings SET modification_timestamp = '2026-06-07 08:00:00' WHERE mls_id = '7001015';
UPDATE listings SET modification_timestamp = '2026-05-15 10:00:00' WHERE mls_id = '7001016';
UPDATE listings SET modification_timestamp = '2026-06-06 13:00:00' WHERE mls_id = '7001017';
UPDATE listings SET modification_timestamp = '2026-06-01 11:00:00' WHERE mls_id = '7001018';
UPDATE listings SET modification_timestamp = '2026-06-07 10:30:00' WHERE mls_id = '7001019';
UPDATE listings SET modification_timestamp = '2026-06-05 15:00:00' WHERE mls_id = '7001020';