package com.aily;

import com.aily.model.CartItem;
import com.aily.model.Order;
import com.aily.model.User;

import java.util.ArrayList;
import java.util.List;

/** Shared state across all screens for the current session. */
public class Session {
    public static User currentUser = null;
    public static List<CartItem> cart = new ArrayList<>();
    public static List<Order> orders = new ArrayList<>();

    // Admin chat detail selection
    public static String adminSelectedChatUserId = null;
    public static String adminSelectedChatUsername = null;

    public static void clear() {
        currentUser = null;
        cart.clear();
        orders.clear();
        adminSelectedChatUserId = null;
        adminSelectedChatUsername = null;
    }

    public static int cartCount() {
        return cart.stream().mapToInt(CartItem::getQuantity).sum();
    }
}
