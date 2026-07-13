#![cfg(test)]

use super::*;
use soroban_sdk::{testutils::{Address as _, Events, Ledger}, Address, Env, Vec, Map};
use soroban_sdk::token::Client as TokenClient;
use soroban_sdk::token::StellarAssetClient as TokenAdminClient;

#[test]
fn test_successful_cycle() {
    let env = Env::default();
    env.mock_all_auths();

    let contract_id = env.register_contract(None, PundarCircleContract);
    let client = PundarCircleContractClient::new(&env, &contract_id);

    let admin = Address::from_string(&env, "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF");
    client.init(&admin);

    let member1 = Address::from_string(&env, "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    let member2 = Address::from_string(&env, "GBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    let member3 = Address::from_string(&env, "GCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

    let members = Vec::from_array(&env, [member1.clone(), member2.clone(), member3.clone()]);
    let payout_order = Vec::from_array(&env, [member1.clone(), member2.clone(), member3.clone()]);
    let amount: i128 = 1000;
    let duration: u64 = 3600;

    let token_admin = Address::from_string(&env, "GDAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    let token_id = env.register_stellar_asset_contract(token_admin.clone());
    let token_client = TokenClient::new(&env, &token_id);
    let token_admin_client = TokenAdminClient::new(&env, &token_id);

    token_admin_client.mint(&member1, &10000);
    token_admin_client.mint(&member2, &10000);
    token_admin_client.mint(&member3, &10000);

    let circle_id = client.create_circle(&members, &amount, &payout_order, &token_id, &duration);
    assert_eq!(circle_id, 1);

    client.contribute(&circle_id, &member1, &amount);
    client.contribute(&circle_id, &member2, &amount);
    client.contribute(&circle_id, &member3, &amount);

    let bal_before = token_client.balance(&member1);
    client.check_and_payout(&circle_id);
    let bal_after = token_client.balance(&member1);
    assert_eq!(bal_after, bal_before + (amount * 3));

    let state = client.get_circle_state(&circle_id);
    assert_eq!(state.current_cycle, 2);
}

#[test]
fn test_dissolve_and_refund() {
    let env = Env::default();
    env.mock_all_auths();

    let contract_id = env.register_contract(None, PundarCircleContract);
    let client = PundarCircleContractClient::new(&env, &contract_id);

    let admin = Address::from_string(&env, "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF");
    client.init(&admin);

    let member1 = Address::from_string(&env, "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    let member2 = Address::from_string(&env, "GBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    let members = Vec::from_array(&env, [member1.clone(), member2.clone()]);
    let amount: i128 = 1000;
    let duration: u64 = 3600;

    let token_admin = Address::from_string(&env, "GDAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    let token_id = env.register_stellar_asset_contract(token_admin.clone());
    let token_client = TokenClient::new(&env, &token_id);
    let token_admin_client = TokenAdminClient::new(&env, &token_id);

    token_admin_client.mint(&member1, &10000);
    token_admin_client.mint(&member2, &10000);

    let circle_id = client.create_circle(&members, &amount, &members, &token_id, &duration);

    // Member 1 contributes
    client.contribute(&circle_id, &member1, &amount);

    // Fast forward time past deadline
    env.ledger().set_timestamp(env.ledger().timestamp() + duration + 1);

    let bal_before = token_client.balance(&member1);
    client.check_and_dissolve(&circle_id);
    let bal_after = token_client.balance(&member1);

    // Member 1 should have been refunded
    assert_eq!(bal_after, bal_before + amount);

    let state = client.get_circle_state(&circle_id);
    assert_eq!(state.dissolved, true);
    assert_eq!(state.pooled_amount, 0);
}

#[test]
#[should_panic(expected = "Circle is dissolved")]
fn test_contribution_after_dissolve() {
    let env = Env::default();
    env.mock_all_auths();

    let contract_id = env.register_contract(None, PundarCircleContract);
    let client = PundarCircleContractClient::new(&env, &contract_id);

    let admin = Address::from_string(&env, "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF");
    client.init(&admin);

    let member1 = Address::from_string(&env, "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    let members = Vec::from_array(&env, [member1.clone()]);
    let amount: i128 = 1000;
    let duration: u64 = 3600;

    let token_admin = Address::from_string(&env, "GDAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    let token_id = env.register_stellar_asset_contract(token_admin.clone());

    let circle_id = client.create_circle(&members, &amount, &members, &token_id, &duration);

    env.ledger().set_timestamp(env.ledger().timestamp() + duration + 1);
    client.check_and_dissolve(&circle_id);

    client.contribute(&circle_id, &member1, &amount); // Should panic
}
