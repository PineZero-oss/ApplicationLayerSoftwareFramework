package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link account#newInstance} factory method to
 * create an instance of this fragment.
 */
public class account extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public account() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment account.
     */
    // TODO: Rename and change types and number of parameters
    public static account newInstance(String param1, String param2) {
        account fragment = new account();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private Button btnLogout;
    private TextView tvUsername, tvEmail;
    private DatabaseHelper databaseHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        databaseHelper = new DatabaseHelper(requireContext());
        tvUsername = view.findViewById(R.id.p_username);
        tvEmail = view.findViewById(R.id.p_Email);

        btnLogout = view.findViewById(R.id.acc_logoutBtn);
        if (btnLogout == null) {
            btnLogout = view.findViewById(R.id.acc_logoutBtn);
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }
        loadUserProfile();

        // Added: Hook up "Sync now" action to sync Firestore and SQLite
        setupSyncNowButton(view);
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentEmail = null;

        if (currentUser != null && currentUser.getEmail() != null) {
            currentEmail = currentUser.getEmail();
        } else if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            currentEmail = prefs.getString("logged_in_email", null);
        }

        if (currentEmail == null) {
            return;
        }

        if (tvEmail != null) {
            tvEmail.setText(currentEmail);
        }

        // 1. Fetch locally from SQLite
        Cursor cursor = databaseHelper.getUserByEmail(currentEmail);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int usernameIndex = cursor.getColumnIndex("username");
                if (usernameIndex != -1 && tvUsername != null) {
                    tvUsername.setText(cursor.getString(usernameIndex));
                }
            }
            cursor.close();
        }

        // 2. Sync from Cloud Firestore if online
        final String syncedEmail = currentEmail;
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && isAdded()) {
                            String cloudUsername = documentSnapshot.getString("username");
                            if (cloudUsername != null && tvUsername != null) {
                                tvUsername.setText(cloudUsername);
                            }

                            // Added: Sync user_type from Firestore -> SQLite (defaulting to 'farmer' if not set yet)
                            String cloudUserType = documentSnapshot.getString("user_type");
                            if (cloudUserType == null) {
                                cloudUserType = documentSnapshot.getString("userType");
                            }
                            if (cloudUserType == null) {
                                Map<String, Object> initRole = new HashMap<>();
                                initRole.put("user_type", DatabaseHelper.ROLE_FARMER);
                                FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(currentUser.getUid())
                                        .set(initRole, SetOptions.merge());
                            }
                            databaseHelper.upsertUserFromCloud(
                                    currentUser.getUid(),
                                    cloudUsername,
                                    syncedEmail,
                                    cloudUserType
                            );
                        }
                    });
        }
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();

        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();

            Intent intent = new Intent(getActivity(), SignIn.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    // ==================== ADDED: SYNC NOW HELPERS ====================

    private void setupSyncNowButton(View root) {
        View syncNowView = findSyncNowView(root);
        if (syncNowView != null) {
            syncNowView.setClickable(true);
            syncNowView.setOnClickListener(v -> {
                syncNowView.setEnabled(false);
                Toast.makeText(requireContext(), "Syncing Firestore & SQLite...", Toast.LENGTH_SHORT).show();

                databaseHelper.syncDatabaseWithFirestore(requireContext(), (success, message) -> {
                    if (!isAdded()) return;
                    syncNowView.setEnabled(true);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    if (success) {
                        loadUserProfile();
                    }
                });
            });
        }
    }

    private View findSyncNowView(View root) {
        String[] candidateIds = {
                "acc_syncBtn", "sync_now", "btn_sync_now", "syncNowBtn", "syncBtn", "tv_sync_now"
        };
        if (getContext() != null) {
            String pkg = getContext().getPackageName();
            for (String idName : candidateIds) {
                int resId = getResources().getIdentifier(idName, "id", pkg);
                if (resId != 0) {
                    View found = root.findViewById(resId);
                    if (found != null) return found;
                }
            }
        }
        return findViewByTextRecursive(root, "sync now");
    }

    private View findViewByTextRecursive(View view, String searchLower) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text.toString().toLowerCase(Locale.US).contains(searchLower)) {
                return view;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View match = findViewByTextRecursive(group.getChildAt(i), searchLower);
                if (match != null) return match;
            }
        }
        return null;
    }
}