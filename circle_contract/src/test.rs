#![cfg(test)]

use super::*;
use soroban_sdk::{
    testutils::{Address as _, Ledger},
    token::Client as TokenClient,
    token::StellarAssetClient as TokenAdminClient,
    Address, Env, Vec,
};

fn setup_env_and_token<'a>(env: &'a Env, members: &[Address]) -> (PundarCircleContractClient<'a>, Address, TokenClient<'a>) {
    let contract_id = env.register(PundarCircleContract, ());
    let client = PundarCircleContractClient::new(env, &contract_id);

    let admin = Address::generate(env);
    client.init(&admin);

    let token_admin = Address::generate(env);
    let token_id = env.register_stellar_asset_contract_v2(token_admin).address();
    let token_client = TokenClient::new(env, &token_id);
    let token_admin_client = TokenAdminClient::new(env, &token_id);

    for m in members {
        token_admin_client.mint(m, &100_000);
    }

    (client, token_id, token_client)
}

#[test]
fn test_full_multi_round_rotation_and_payout_derivation() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let member2 = Address::generate(&env);
    let member3 = Address::generate(&env);
    let (client, token_id, token_client) =
        setup_env_and_token(&env, &[member1.clone(), member2.clone(), member3.clone()]);

    let members = Vec::from_array(&env, [member1.clone(), member2.clone(), member3.clone()]);
    let payout_order = Vec::from_array(&env, [member1.clone(), member2.clone(), member3.clone()]);
    let amount: i128 = 1_000;
    let duration: u64 = 3_600;

    let circle_id = client.create_circle(&members, &amount, &payout_order, &token_id, &duration);
    assert_eq!(circle_id, 1);

    // ── Round 1: Recipient is member1, derived payout = 1000 * 3 = 3000 ──
    client.contribute(&circle_id, &member1, &amount);
    client.contribute(&circle_id, &member2, &amount);
    client.contribute(&circle_id, &member3, &amount);

    let m1_before = token_client.balance(&member1);
    client.check_and_payout(&circle_id);
    assert_eq!(token_client.balance(&member1), m1_before + (amount * 3));

    let state_r1 = client.get_circle_state(&circle_id);
    assert_eq!(state_r1.current_cycle, 2);
    assert_eq!(state_r1.next_payout_index, 1);
    assert_eq!(state_r1.pooled_amount, 0);
    assert!(!state_r1.completed);

    // ── Round 2: Recipient is member2, derived payout = 1000 * 3 = 3000 ──
    client.contribute(&circle_id, &member1, &amount);
    client.contribute(&circle_id, &member2, &amount);
    client.contribute(&circle_id, &member3, &amount);

    let m2_before = token_client.balance(&member2);
    client.check_and_payout(&circle_id);
    assert_eq!(token_client.balance(&member2), m2_before + (amount * 3));

    let state_r2 = client.get_circle_state(&circle_id);
    assert_eq!(state_r2.current_cycle, 3);
    assert_eq!(state_r2.next_payout_index, 2);
    assert!(!state_r2.completed);

    // ── Round 3: Recipient is member3, derived payout = 1000 * 3 = 3000 ──
    client.contribute(&circle_id, &member1, &amount);
    client.contribute(&circle_id, &member2, &amount);
    client.contribute(&circle_id, &member3, &amount);

    let m3_before = token_client.balance(&member3);
    client.check_and_payout(&circle_id);
    assert_eq!(token_client.balance(&member3), m3_before + (amount * 3));

    let state_final = client.get_circle_state(&circle_id);
    assert!(state_final.completed);
    assert_eq!(state_final.paid_rounds.len(), 3);
}

#[test]
#[should_panic(expected = "Address is not a member of this circle")]
fn test_reject_non_member_contribution() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let member2 = Address::generate(&env);
    let outsider = Address::generate(&env);
    let (client, token_id, _) =
        setup_env_and_token(&env, &[member1.clone(), member2.clone(), outsider.clone()]);

    let members = Vec::from_array(&env, [member1.clone(), member2.clone()]);
    let circle_id = client.create_circle(&members, &1_000, &members, &token_id, &3_600);

    // Non-member attempts to contribute
    client.contribute(&circle_id, &outsider, &1_000);
}

#[test]
#[should_panic(expected = "Invalid contribution amount")]
fn test_reject_partial_contribution_amount() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let member2 = Address::generate(&env);
    let (client, token_id, _) = setup_env_and_token(&env, &[member1.clone(), member2.clone()]);

    let members = Vec::from_array(&env, [member1.clone(), member2.clone()]);
    let circle_id = client.create_circle(&members, &1_000, &members, &token_id, &3_600);

    // Partial contribution (500 instead of locked 1000) must be rejected
    client.contribute(&circle_id, &member1, &500);
}

#[test]
#[should_panic(expected = "Invalid contribution amount")]
fn test_reject_overpaid_contribution_amount() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let member2 = Address::generate(&env);
    let (client, token_id, _) = setup_env_and_token(&env, &[member1.clone(), member2.clone()]);

    let members = Vec::from_array(&env, [member1.clone(), member2.clone()]);
    let circle_id = client.create_circle(&members, &1_000, &members, &token_id, &3_600);

    // Overpaid contribution (1500 instead of locked 1000) must be rejected
    client.contribute(&circle_id, &member1, &1_500);
}

#[test]
#[should_panic(expected = "Member already contributed this cycle")]
fn test_reject_duplicate_contribution_same_round() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let member2 = Address::generate(&env);
    let (client, token_id, _) = setup_env_and_token(&env, &[member1.clone(), member2.clone()]);

    let members = Vec::from_array(&env, [member1.clone(), member2.clone()]);
    let circle_id = client.create_circle(&members, &1_000, &members, &token_id, &3_600);

    client.contribute(&circle_id, &member1, &1_000);
    // Second contribution in the same round must panic
    client.contribute(&circle_id, &member1, &1_000);
}

#[test]
#[should_panic(expected = "Not all members have contributed for this cycle")]
fn test_reject_payout_with_incomplete_contributions() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let member2 = Address::generate(&env);
    let member3 = Address::generate(&env);
    let (client, token_id, _) =
        setup_env_and_token(&env, &[member1.clone(), member2.clone(), member3.clone()]);

    let members = Vec::from_array(&env, [member1.clone(), member2.clone(), member3.clone()]);
    let circle_id = client.create_circle(&members, &1_000, &members, &token_id, &3_600);

    // Only 2 of 3 members contribute
    client.contribute(&circle_id, &member1, &1_000);
    client.contribute(&circle_id, &member2, &1_000);

    // Premature payout attempt must panic
    client.check_and_payout(&circle_id);
}

#[test]
#[should_panic(expected = "Circle has already completed all payout rounds")]
fn test_reject_duplicate_payout_after_completion() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let member2 = Address::generate(&env);
    let (client, token_id, _) = setup_env_and_token(&env, &[member1.clone(), member2.clone()]);

    let members = Vec::from_array(&env, [member1.clone(), member2.clone()]);
    let circle_id = client.create_circle(&members, &1_000, &members, &token_id, &3_600);

    // Round 1
    client.contribute(&circle_id, &member1, &1_000);
    client.contribute(&circle_id, &member2, &1_000);
    client.check_and_payout(&circle_id);

    // Round 2
    client.contribute(&circle_id, &member1, &1_000);
    client.contribute(&circle_id, &member2, &1_000);
    client.check_and_payout(&circle_id);

    // Duplicate payout after all rounds completed must panic
    client.check_and_payout(&circle_id);
}

#[test]
#[should_panic(expected = "Circle must have at least two members")]
fn test_reject_single_member_circle() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let (client, token_id, _) = setup_env_and_token(&env, &[member1.clone()]);

    let members = Vec::from_array(&env, [member1.clone()]);
    client.create_circle(&members, &1_000, &members, &token_id, &3_600);
}

#[test]
#[should_panic(expected = "Duplicate member address is not allowed")]
fn test_reject_duplicate_members_in_circle() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let (client, token_id, _) = setup_env_and_token(&env, &[member1.clone()]);

    let members = Vec::from_array(&env, [member1.clone(), member1.clone()]);
    client.create_circle(&members, &1_000, &members, &token_id, &3_600);
}

#[test]
#[should_panic(expected = "Payout order contains non-member address")]
fn test_reject_payout_order_with_non_member() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let member2 = Address::generate(&env);
    let outsider = Address::generate(&env);
    let (client, token_id, _) = setup_env_and_token(&env, &[member1.clone(), member2.clone()]);

    let members = Vec::from_array(&env, [member1.clone(), member2.clone()]);
    let invalid_payout_order = Vec::from_array(&env, [member1.clone(), outsider]);
    client.create_circle(&members, &1_000, &invalid_payout_order, &token_id, &3_600);
}

#[test]
fn test_dissolve_and_refund() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let member2 = Address::generate(&env);
    let (client, token_id, token_client) =
        setup_env_and_token(&env, &[member1.clone(), member2.clone()]);

    let members = Vec::from_array(&env, [member1.clone(), member2.clone()]);
    let amount: i128 = 1_000;
    let duration: u64 = 3_600;

    let circle_id = client.create_circle(&members, &amount, &members, &token_id, &duration);

    // Member 1 contributes
    client.contribute(&circle_id, &member1, &amount);

    // Fast forward time past deadline
    env.ledger().set_timestamp(env.ledger().timestamp() + duration + 1);

    let bal_before = token_client.balance(&member1);
    client.check_and_dissolve(&circle_id);
    let bal_after = token_client.balance(&member1);

    // Member 1 is refunded their locked contribution
    assert_eq!(bal_after, bal_before + amount);

    let state = client.get_circle_state(&circle_id);
    assert!(state.dissolved);
    assert_eq!(state.pooled_amount, 0);
}

#[test]
#[should_panic(expected = "Circle is dissolved")]
fn test_contribution_after_dissolve() {
    let env = Env::default();
    env.mock_all_auths();

    let member1 = Address::generate(&env);
    let member2 = Address::generate(&env);
    let (client, token_id, _) = setup_env_and_token(&env, &[member1.clone(), member2.clone()]);

    let members = Vec::from_array(&env, [member1.clone(), member2.clone()]);
    let amount: i128 = 1_000;
    let duration: u64 = 3_600;

    let circle_id = client.create_circle(&members, &amount, &members, &token_id, &duration);

    env.ledger().set_timestamp(env.ledger().timestamp() + duration + 1);
    client.check_and_dissolve(&circle_id);

    client.contribute(&circle_id, &member1, &amount);
}
