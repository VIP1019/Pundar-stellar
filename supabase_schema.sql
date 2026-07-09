-- PUNDAR App Supabase Schema
-- Run this entire script in your Supabase SQL Editor

-- 1. Create custom enum types
CREATE TYPE bill_status AS ENUM ('SETTLED', 'PENDING', 'PARTIAL');
CREATE TYPE contrib_status AS ENUM ('PAID', 'PENDING', 'OVERDUE');
CREATE TYPE activity_type AS ENUM ('AUTO_SWEEP', 'DIVIDEND', 'PURCHASE', 'ROUND_UP', 'PAYOUT');

-- 2. Profiles (Users)
CREATE TABLE profiles (
    id UUID REFERENCES auth.users(id) PRIMARY KEY,
    phone_number TEXT UNIQUE,
    name TEXT NOT NULL,
    initials TEXT NOT NULL,
    pundar_score INTEGER DEFAULT 800,
    avatar_color BIGINT DEFAULT 4278211788, -- 0xFF0052CC in decimal
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Secure Profiles with RLS (Row Level Security)
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view all profiles" ON profiles FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Users can insert own profile" ON profiles FOR INSERT WITH CHECK (auth.uid() = id);


-- 3. Group Bills (Pay)
CREATE TABLE group_bills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID REFERENCES profiles(id) NOT NULL,
    name TEXT NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    member_count INTEGER NOT NULL,
    status bill_status DEFAULT 'PENDING',
    date TEXT NOT NULL,
    your_share DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE group_bills ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view bills they created" ON group_bills FOR SELECT USING (auth.uid() = creator_id);
CREATE POLICY "Users can insert bills" ON group_bills FOR INSERT WITH CHECK (auth.uid() = creator_id);
CREATE POLICY "Users can update own bills" ON group_bills FOR UPDATE USING (auth.uid() = creator_id);


-- 4. Bill Members
CREATE TABLE bill_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id UUID REFERENCES group_bills(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    username TEXT,
    initials TEXT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    avatar_color BIGINT DEFAULT 4285231744, -- 0xFF6B7280 in decimal
    is_you BOOLEAN DEFAULT false
);

ALTER TABLE bill_members ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view bill members" ON bill_members FOR SELECT USING (true);
CREATE POLICY "Users can insert bill members" ON bill_members FOR INSERT WITH CHECK (true);


-- 5. Circles (Save)
CREATE TABLE circles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID REFERENCES profiles(id) NOT NULL,
    name TEXT NOT NULL,
    target_amount DECIMAL(12,2) NOT NULL,
    saved_amount DECIMAL(12,2) DEFAULT 0,
    target_date TEXT NOT NULL,
    member_count INTEGER NOT NULL,
    contribution_per_month DECIMAL(12,2) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    escrow_address TEXT DEFAULT '0x7f...9A2B',
    escrow_network TEXT DEFAULT 'Stellar Network',
    monthly_due_day INTEGER DEFAULT 5,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE circles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view active circles" ON circles FOR SELECT USING (true);
CREATE POLICY "Users can insert circles" ON circles FOR INSERT WITH CHECK (auth.uid() = creator_id);
CREATE POLICY "Users can update circles" ON circles FOR UPDATE USING (true);


-- 6. Circle Members
CREATE TABLE circle_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    circle_id UUID REFERENCES circles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    initials TEXT NOT NULL,
    share_percent INTEGER NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status contrib_status DEFAULT 'PENDING',
    is_you BOOLEAN DEFAULT false,
    avatar_color BIGINT DEFAULT 4285231744
);

ALTER TABLE circle_members ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view circle members" ON circle_members FOR SELECT USING (true);
CREATE POLICY "Users can insert circle members" ON circle_members FOR INSERT WITH CHECK (true);
CREATE POLICY "Users can update circle members" ON circle_members FOR UPDATE USING (true);


-- 7. Portfolios (Grow)
CREATE TABLE portfolios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES profiles(id) NOT NULL UNIQUE,
    total_value DECIMAL(12,2) DEFAULT 0,
    total_return_percent DECIMAL(5,2) DEFAULT 0,
    total_return_amount DECIMAL(12,2) DEFAULT 0,
    ph_equities_percent INTEGER DEFAULT 60,
    us_equities_percent INTEGER DEFAULT 25,
    fixed_income_percent INTEGER DEFAULT 15,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE portfolios ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view own portfolio" ON portfolios FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can update own portfolio" ON portfolios FOR UPDATE USING (auth.uid() = user_id);


-- 8. Portfolio Activities
CREATE TABLE portfolio_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id UUID REFERENCES portfolios(id) ON DELETE CASCADE,
    type activity_type NOT NULL,
    description TEXT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    date TEXT NOT NULL,
    is_positive BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE portfolio_activities ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view own activities" ON portfolio_activities FOR SELECT USING (true);
CREATE POLICY "Users can insert activities" ON portfolio_activities FOR INSERT WITH CHECK (true);


-- 9. Home Activities
CREATE TABLE home_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES profiles(id) NOT NULL,
    icon TEXT NOT NULL,
    title TEXT NOT NULL,
    subtitle TEXT NOT NULL,
    amount TEXT NOT NULL,
    is_positive BOOLEAN NOT NULL,
    module TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE home_activities ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view own home activities" ON home_activities FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert home activities" ON home_activities FOR INSERT WITH CHECK (auth.uid() = user_id);
