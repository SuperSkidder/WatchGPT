package me.superskidder.watchgpt.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import me.superskidder.watchgpt.MainFragment
import me.superskidder.watchgpt.SettingsFragment

class MainFragmentAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MainFragment.newInstance()
            1 -> SettingsFragment.newInstance()
            else -> throw IllegalStateException("Unexpected position: $position")
        }
    }
}
