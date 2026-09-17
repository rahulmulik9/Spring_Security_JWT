INSERT INTO users (username, password, role) VALUES
('Ajay', '$2b$10$.8fP4TPs0lF1pAfFR4X.3u0YyIbninbdfP9n0lWKiFe5Q2FVwC3/e', 'USER'),
('Rahul', '$2b$10$KXM3fu.YdxthgmYU86bTReXUU00B5Gt3DCvwi3e.ovVho3lWnP/jG', 'ADMIN');


INSERT INTO tasks (title, description, completed, created_at) VALUES
('Buy groceries', 'Milk, eggs, bread, butter', false, now()),
('Finish Spring Security plan', 'Work through Phase 1 to Phase 10', false, now()),
('Clean the apartment', 'Vacuum, dishes, laundry', true, now()),
('Read a book', 'Finish current chapter of Clean Code', false, now()),
('Book dentist appointment', 'Check availability for next week', false, now()),
('Renew car insurance', 'Compare quotes before renewal date', true, now());