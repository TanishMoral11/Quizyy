module MyModule::quiz {
    use std::signer;
    use aptos_framework::coin;
    use aptos_framework::aptos_coin::AptosCoin;
    use std::vector;

    // Error codes
    const E_NOT_INITIALIZED: u64 = 1;
    const E_ALREADY_INITIALIZED: u64 = 2;
    const E_NOT_OWNER: u64 = 3;
    const E_QUIZ_NOT_COMPLETED: u64 = 4;
    const E_INSUFFICIENT_BALANCE: u64 = 5;

    struct QuizData has key {
        owner: address,
        price: u64,
        completed_quizzes: vector<address>,
    }

    // Initialize the quiz
    public entry fun initialize(account: &signer, price: u64) {
        let account_addr = signer::address_of(account);
        assert!(!exists<QuizData>(account_addr), E_ALREADY_INITIALIZED);

        move_to(account, QuizData {
            owner: account_addr,
            price,
            completed_quizzes: vector::empty<address>(),
        });
    }

    // Take the quiz
    public entry fun take_quiz(account: &signer, owner_addr: address) acquires QuizData {
        let account_addr = signer::address_of(account);
        assert!(exists<QuizData>(owner_addr), E_NOT_INITIALIZED);

        let quiz_data = borrow_global_mut<QuizData>(owner_addr);
        let price = quiz_data.price;

        // Withdraw the price and deposit it to the owner's account
        coin::transfer<AptosCoin>(account, quiz_data.owner, price);

        // Add user to completed quizzes
        vector::push_back(&mut quiz_data.completed_quizzes, account_addr);
    }

    // Helper function to check if a user has completed the quiz
    fun has_completed_quiz(quiz_data: &QuizData, account_addr: address): bool {
        vector::contains(&quiz_data.completed_quizzes, &account_addr)
    }

    // Submit quiz score and potentially refund
    public entry fun submit_score(account: &signer, owner_addr: address, score: u8) acquires QuizData {
        let account_addr = signer::address_of(account);
        assert!(exists<QuizData>(owner_addr), E_NOT_INITIALIZED);

        let quiz_data = borrow_global_mut<QuizData>(owner_addr);
        assert!(has_completed_quiz(quiz_data, account_addr), E_QUIZ_NOT_COMPLETED);

        // Refund if score is 3 or higher
        if (score >= 3) {
            assert!(
                coin::balance<AptosCoin>(owner_addr) >= quiz_data.price,
                E_INSUFFICIENT_BALANCE
            );
            coin::transfer<AptosCoin>(account, owner_addr, quiz_data.price);
        };

        // Remove user from completed quizzes
        let (found, index) = vector::index_of(&quiz_data.completed_quizzes, &account_addr);
        if (found) {
            vector::remove(&mut quiz_data.completed_quizzes, index);
        };
    }

    // Update quiz price (only the owner can update)
    public entry fun update_price(account: &signer, new_price: u64) acquires QuizData {
        let account_addr = signer::address_of(account);
        assert!(exists<QuizData>(account_addr), E_NOT_INITIALIZED);

        let quiz_data = borrow_global_mut<QuizData>(account_addr);
        assert!(account_addr == quiz_data.owner, E_NOT_OWNER);

        quiz_data.price = new_price;
    }

    // Get quiz price
    #[view]
    public fun get_price(account_addr: address): u64 acquires QuizData {
        assert!(exists<QuizData>(account_addr), E_NOT_INITIALIZED);
        borrow_global<QuizData>(account_addr).price
    }
}