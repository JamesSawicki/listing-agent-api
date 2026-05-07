-- data.sql - Spring Boot runs this automatically after creating the schema.
-- This gives you real data to work with immediately on startup.
-- Add your actual listings here.

INSERT INTO listings (
    address, city_state_zip, neighborhood, price, style, year_built,
    beds, baths, sqft, lot_sqft, garage, taxes, estimated_payment,
    agent_name, description, features, location
) VALUES (
    '1847 Goodrich Ave',
    'Saint Paul, MN 55105',
    'Crocus Hill',
    '$685,000',
    'Craftsman Bungalow',
    '1924',
    '4', '2.5', '2,840', '7,840',
    '2-car detached',
    '$9,840/yr (~$820/mo)',
    '~$3,620/mo P&I (20% down, 6.9% rate)',
    'Jim',
    'A beautifully preserved 1924 Craftsman bungalow on one of Crocus Hill''s most sought-after blocks. Original character meets thoughtful renovation - from the wraparound front porch to the chef''s kitchen.',
    'Original hardwood floors throughout
Renovated kitchen - quartz countertops, stainless appliances
Primary suite with spa bath (renovated 2020)
Wood-burning fireplace in living room
Formal dining room with original built-ins
Finished basement - family room + storage
Wraparound front porch (original)
New roof (2022) . Updated electrical (2021)
Forced air + zoned radiant heat . Central A/C
School district: Saint Paul Public',
    '3 blocks to Grand Ave dining & shopping
0.5 mi to I-35E . 0.8 mi to Whole Foods
Walkability score: 82 / 100'
);

INSERT INTO listings (
    address, city_state_zip, neighborhood, price, style, year_built,
    beds, baths, sqft, lot_sqft, garage, taxes, estimated_payment,
    agent_name, description, features, location
) VALUES (
    '2241 Fairmount Ave',
    'Saint Paul, MN 55105',
    'Mac-Groveland',
    '$549,000',
    'Colonial Revival',
    '1938',
    '3', '2', '1,980', '6,500',
    '1-car attached',
    '$7,200/yr (~$600/mo)',
    '~$2,890/mo P&I (20% down, 6.9% rate)',
    'Jim',
    'A stately 1938 Colonial Revival on a quiet Mac-Groveland block. Thoughtfully updated while preserving the original millwork, arched doorways, and coved ceilings that define the period.',
    'Original millwork and arched doorways throughout
Updated kitchen with granite countertops
Coved ceilings in living and dining rooms
Three-season porch off the back
Hardwood floors on main level
Updated bathrooms (2019)
Full unfinished basement - excellent storage
Newer windows (2018) . Roof (2017)',
    '0.4 mi to Randolph Ave shops and restaurants
0.6 mi to St. Kate''s campus
Easy access to I-35E and Hwy 5
Walkability score: 74 / 100'
);
