package cleveroad.com.lib.widget;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.List;

import cleveroad.com.lib.R;
import cleveroad.com.lib.adapter.CategoriesAdapter;
import static cleveroad.com.lib.adapter.CategoriesAdapter.ICategoryItem;
import cleveroad.com.lib.model.MockedItemsFactory;
import cleveroad.com.lib.util.AbstractAnimatorListener;

public class EndlessNavigationView extends FrameLayout implements OnItemClickListener<CategoriesAdapter.ICategoryItem> {
    private static final String TAG = EndlessNavigationView.class.getSimpleName();
    public static final int ORIENTATION_VERTICAL = 0;
    public static final int ORIENTATION_HORIZONTAL = 1;

    private Animator selectionInAnimator;
    private Animator selectionOutAnimator;

    private FrameLayout flContainerSelected;
    private RecyclerView rvCategories;
    private CategoriesAdapter.CategoriesHolder categoriesHolder;
    private CategoriesAdapter categoriesAdapter;
    List<ICategoryItem> items;

    int realHidedPosition = 0;

    public EndlessNavigationView(Context context) {
        super(context);
        init(context, null);
    }

    public EndlessNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public EndlessNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EndlessNavigationView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void inflate(){
        View view = inflate(getContext(), R.layout.view_categories_navigation, this);
        flContainerSelected = (FrameLayout) view.findViewById(R.id.flContainerSelected);
        rvCategories = (RecyclerView) view.findViewById(R.id.rvCategories);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        inflate();

        //read customization attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EndlessNavigationView);
        int colorListBackground = a.getColor(R.styleable.EndlessNavigationView_listBackground,
                ContextCompat.getColor(getContext(), R.color.default_list_background));
        int colorSelectionView = a.getColor(R.styleable.EndlessNavigationView_selectionBackground,
                ContextCompat.getColor(getContext(), R.color.default_selection_view_background));
        int orientation = a.getInteger(R.styleable.EndlessNavigationView_orientation, ORIENTATION_HORIZONTAL);
        int selectionAnimatorInId = a.getResourceId(R.styleable.EndlessNavigationView_selectionInAnimation, R.animator.scale_restore);
        int selectionAnimatorOutId = a.getResourceId(R.styleable.EndlessNavigationView_selectionOutAnimation, R.animator.scale_small);
        a.recycle();
        selectionInAnimator = AnimatorInflater.loadAnimator(getContext(), selectionAnimatorInId);
        selectionOutAnimator = AnimatorInflater.loadAnimator(getContext(), selectionAnimatorOutId);

        //current view has two state : horizontal & vertical. State design pattern
        OrientationState orientationState = getOrientationStateFromParam(orientation);
        LinearLayoutManager linearLayoutManager = orientationState.getLayoutManager(getContext());
        rvCategories.setLayoutManager(linearLayoutManager);

        rvCategories.setBackgroundColor(colorListBackground);
        flContainerSelected.setBackgroundColor(colorSelectionView);

        items = MockedItemsFactory.getCategoryItems();
        items.get(0).setVisible(false);

        categoriesAdapter = new CategoriesAdapter(items, this);
        rvCategories.setAdapter(categoriesAdapter);

        View itemView = CategoriesAdapter.createView(flContainerSelected);
        categoriesHolder = CategoriesAdapter.CategoriesHolder.newBuilder(itemView).build();
        categoriesHolder.bindItem(items.get(0));
        flContainerSelected.addView(itemView);

        //scroll to middle of indeterminate recycler view on initialization and if user somehow scrolled to start or end
        linearLayoutManager.scrollToPositionWithOffset(Integer.MAX_VALUE / 2, getResources().getDimensionPixelOffset(R.dimen.selected_view_size_plus_margin));
        rvCategories.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (linearLayoutManager.findFirstVisibleItemPosition() == 0 || linearLayoutManager.findFirstVisibleItemPosition() == Integer.MAX_VALUE) {
                    linearLayoutManager.scrollToPosition(Integer.MAX_VALUE/2);
                }
            }
        });
    }

    private void startSelectedViewOutAnimation(final ICategoryItem item){
        Animator animator = selectionOutAnimator;
        animator.setTarget(categoriesHolder.itemView);
        animator.start();
        animator.addListener(new AbstractAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //replace selected view
                categoriesHolder.bindItem(item);
                startSelectedViewInAnimation();
            }
        });
    }

    private void startSelectedViewInAnimation() {
        Animator animator = selectionInAnimator;
        animator.setTarget(categoriesHolder.itemView);
        animator.start();
    }

    @Override
    public void onItemClicked(CategoriesAdapter.ICategoryItem item, int position) {
        ICategoryItem oldHidedItem = items.get(realHidedPosition);

        int realPosition = position % items.size();
        int itemToShowAdapterPosition = position - realPosition + realHidedPosition;

        item.setVisible(false);

        startSelectedViewOutAnimation(item);

        categoriesAdapter.notifyItemChanged(position);
        realHidedPosition = realPosition;

        oldHidedItem.setVisible(true);
        categoriesAdapter.notifyItemChanged(itemToShowAdapterPosition);

        Log.i(TAG, "clicked on position =" + position);
    }

    public OrientationState getOrientationStateFromParam(int orientation){
        return orientation == 0 ? new OrientationStateVertical() : new OrientationStateHorizontal();
    }

    private interface OrientationState {
        LinearLayoutManager getLayoutManager(Context context);
    }

    private class OrientationStateVertical implements OrientationState {
        @Override
        public LinearLayoutManager getLayoutManager(Context context) {
            return new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        }
    }

    private class OrientationStateHorizontal implements OrientationState {

        @Override
        public LinearLayoutManager getLayoutManager(Context context) {
            return new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        }
    }
}
