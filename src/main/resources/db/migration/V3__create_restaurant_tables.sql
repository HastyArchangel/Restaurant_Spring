-- Ensure we are using the correct schema
SET search_path = project, pg_catalog;

-- Create Dish table (assuming it wasn't in V1 or V2)
-- Check your existing migrations first! If Dish exists, remove this part.
CREATE TABLE dish (
                      id uuid NOT NULL,
                      name character varying(255) NOT NULL, -- Added NOT NULL constraint
                      description text, -- Changed to text for potentially longer descriptions
                      price numeric(10, 2) NOT NULL, -- Use numeric for currency, added NOT NULL
                      CONSTRAINT dish_pkey PRIMARY KEY (id)
);

-- Create Order table
-- Using quotes around "order" as it's a reserved keyword in SQL
CREATE TABLE "order" (
                         id uuid NOT NULL,
                         order_date timestamp without time zone NOT NULL, -- Added NOT NULL
                         status character varying(50) NOT NULL, -- Added NOT NULL, maybe restrict length
                         user_id uuid NOT NULL, -- Renamed from client_id for consistency, added NOT NULL
                         CONSTRAINT order_pkey PRIMARY KEY (id),
                         CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT -- Prevent deleting user with orders? Or SET NULL/CASCADE?
);

-- Create Order_Dish join table (for ManyToMany between Order and Dish)
CREATE TABLE order_dish (
                            order_id uuid NOT NULL,
                            dish_id uuid NOT NULL,
                            CONSTRAINT order_dish_pkey PRIMARY KEY (order_id, dish_id),
                            CONSTRAINT fk_order_dish_order FOREIGN KEY (order_id) REFERENCES "order"(id) ON DELETE CASCADE, -- If order is deleted, remove link
                            CONSTRAINT fk_order_dish_dish FOREIGN KEY (dish_id) REFERENCES dish(id) ON DELETE RESTRICT -- Prevent deleting dish if it's in an order?
);

-- Create Review table
CREATE TABLE review (
                        id uuid NOT NULL,
                        rating integer NOT NULL CHECK (rating >= 1 AND rating <= 5), -- Added constraint for rating 1-5
                        comment text,
                        review_date timestamp without time zone NOT NULL, -- Added NOT NULL
                        user_id uuid NOT NULL, -- Renamed from reviewer_id, added NOT NULL
                        dish_id uuid NOT NULL, -- Added NOT NULL
                        CONSTRAINT review_pkey PRIMARY KEY (id),
                        CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, -- If user deleted, remove their reviews
                        CONSTRAINT fk_review_dish FOREIGN KEY (dish_id) REFERENCES dish(id) ON DELETE CASCADE, -- If dish deleted, remove its reviews?
                        CONSTRAINT uk_review_user_dish UNIQUE (user_id, dish_id) -- Add unique constraint: one user review per dish
);

-- Add indexes for foreign keys used in lookups (optional but recommended for performance)
CREATE INDEX idx_order_user_id ON "order"(user_id);
CREATE INDEX idx_order_dish_dish_id ON order_dish(dish_id);
CREATE INDEX idx_review_user_id ON review(user_id);
CREATE INDEX idx_review_dish_id ON review(dish_id);