#![no_std]
use soroban_sdk::{contract, contractimpl, contracttype, Address, Env, Map, Vec, token};

#[contracttype]
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct CircleConfig {
    pub members: Vec<Address>,
    pub contribution_amount: i128,
    pub payout_order: Vec<Address>,
    pub token_address: Address,
    pub cycle_duration: u64, // in seconds
}

#[contracttype]
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct CircleState {
    pub current_cycle: u32,
    pub contributions: Map<Address, bool>,
    pub next_payout_index: u32,
    pub pooled_amount: i128,
    pub cycle_deadline: u64, // timestamp
    pub dissolved: bool,
}

#[contracttype]
pub enum DataKey {
    CircleCount,
    CircleCfg(u64),
    CircleSt(u64),
    Admin,
}

#[contract]
pub struct PundarCircleContract;

#[contractimpl]
impl PundarCircleContract {
    pub fn init(env: Env, admin: Address) {
        if env.storage().instance().has(&DataKey::Admin) {
            panic!("Already initialized");
        }
        env.storage().instance().set(&DataKey::Admin, &admin);
        env.storage().instance().set(&DataKey::CircleCount, &0u64);
    }

    pub fn create_circle(env: Env, members: Vec<Address>, contribution_amount: i128, payout_order: Vec<Address>, token_address: Address, cycle_duration: u64) -> u64 {
        let mut count: u64 = env.storage().instance().get(&DataKey::CircleCount).unwrap_or(0);
        count += 1;

        let cfg = CircleConfig {
            members: members.clone(),
            contribution_amount,
            payout_order,
            token_address,
            cycle_duration,
        };

        let mut contributions = Map::new(&env);
        for m in members.iter() {
            contributions.set(m, false);
        }

        let state = CircleState {
            current_cycle: 1,
            contributions,
            next_payout_index: 0,
            pooled_amount: 0,
            cycle_deadline: env.ledger().timestamp() + cycle_duration,
            dissolved: false,
        };

        env.storage().persistent().set(&DataKey::CircleCfg(count), &cfg);
        env.storage().persistent().set(&DataKey::CircleSt(count), &state);
        env.storage().instance().set(&DataKey::CircleCount, &count);

        count
    }

    pub fn contribute(env: Env, circle_id: u64, member: Address, amount: i128) {
        member.require_auth();

        let cfg: CircleConfig = env.storage().persistent().get(&DataKey::CircleCfg(circle_id)).expect("Circle not found");
        let mut state: CircleState = env.storage().persistent().get(&DataKey::CircleSt(circle_id)).expect("State not found");

        if state.dissolved {
            panic!("Circle is dissolved");
        }

        if amount != cfg.contribution_amount {
            panic!("Invalid contribution amount");
        }

        if !cfg.members.contains(&member) {
            panic!("Address is not a member of this circle");
        }

        if state.contributions.get(member.clone()).unwrap_or(false) {
            panic!("Member already contributed this cycle");
        }

        let client = token::Client::new(&env, &cfg.token_address);
        client.transfer(&member, &env.current_contract_address(), &amount);

        state.contributions.set(member, true);
        state.pooled_amount += amount;
        env.storage().persistent().set(&DataKey::CircleSt(circle_id), &state);
    }

    pub fn check_and_payout(env: Env, circle_id: u64) {
        let cfg: CircleConfig = env.storage().persistent().get(&DataKey::CircleCfg(circle_id)).expect("Circle not found");
        let mut state: CircleState = env.storage().persistent().get(&DataKey::CircleSt(circle_id)).expect("State not found");

        if state.dissolved {
            panic!("Circle is dissolved");
        }

        let mut all_contributed = true;
        for m in cfg.members.iter() {
            if !state.contributions.get(m).unwrap_or(false) {
                all_contributed = false;
                break;
            }
        }

        if !all_contributed {
            panic!("Not all members have contributed for this cycle");
        }

        let recipient = cfg.payout_order.get(state.next_payout_index).expect("Payout order error");
        let amount_to_pay = state.pooled_amount;

        let client = token::Client::new(&env, &cfg.token_address);
        client.transfer(&env.current_contract_address(), &recipient, &amount_to_pay);

        state.current_cycle += 1;
        state.pooled_amount = 0;
        state.next_payout_index = (state.next_payout_index + 1) % cfg.payout_order.len();
        state.cycle_deadline = env.ledger().timestamp() + cfg.cycle_duration;

        let mut next_contributions = Map::new(&env);
        for m in cfg.members.iter() {
            next_contributions.set(m, false);
        }
        state.contributions = next_contributions;

        env.storage().persistent().set(&DataKey::CircleSt(circle_id), &state);
    }

    pub fn check_and_dissolve(env: Env, circle_id: u64) {
        let cfg: CircleConfig = env.storage().persistent().get(&DataKey::CircleCfg(circle_id)).expect("Circle not found");
        let mut state: CircleState = env.storage().persistent().get(&DataKey::CircleSt(circle_id)).expect("State not found");

        if state.dissolved {
            panic!("Circle is already dissolved");
        }

        if env.ledger().timestamp() < state.cycle_deadline {
            panic!("Cycle deadline has not passed yet");
        }

        let client = token::Client::new(&env, &cfg.token_address);
        for m in cfg.members.iter() {
            if state.contributions.get(m.clone()).unwrap_or(false) {
                client.transfer(&env.current_contract_address(), &m, &cfg.contribution_amount);
            }
        }

        state.dissolved = true;
        state.pooled_amount = 0;
        env.storage().persistent().set(&DataKey::CircleSt(circle_id), &state);
    }

    pub fn get_circle_state(env: Env, circle_id: u64) -> CircleState {
        env.storage().persistent().get(&DataKey::CircleSt(circle_id)).expect("State not found")
    }
}

#[cfg(test)]
mod test;
