package green.liam.base;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import green.liam.events.EventManager;
import green.liam.events.EventManagerFactory;
import green.liam.events.Observer;
import green.liam.events.TransformChangeEvent;
import green.liam.events.TransformChangeEvent.ChangeType;
import green.liam.rendering.Camera;
import green.liam.util.Helper;
import processing.core.PMatrix2D;
import processing.core.PVector;

public class Transform extends Component {
    protected static final Transform IDENTITY = new RootTransform();

    protected final EventManager<TransformChangeEvent> changeEventManager =
            EventManagerFactory.getEventManager(TransformChangeEvent.class);

    protected Transform parent;
    protected PVector position = new PVector();
    protected float height = 0f;
    protected float rotation = 0f;
    protected PVector scale = new PVector(1, 1, 1);
    protected Set<Transform> children = new HashSet<>();

    protected PMatrix2D scaleMatrix = new PMatrix2D();
    protected PMatrix2D rotationMatrix = new PMatrix2D();
    protected PMatrix2D translationMatrix = new PMatrix2D();
    protected PMatrix2D combinedMatrix = new PMatrix2D();

    public Transform(GameObject gameObject, Transform parent) {
        super(gameObject);
        this.parent = parent;
        this.parent.addChild(this);
        this.recalculateMatrix();
    }

    public Transform(GameObject gameObject) {
        this(gameObject, IDENTITY);
    }

    public Transform getParent() {
        return this.parent;
    }

    public Set<Transform> getChildren() {
        return Collections.unmodifiableSet(this.children);
    }

    public void addChild(Transform child) {
        this.children.add(child);
        child.setParent(this);
    }

    public void removeChild(Transform child) {
        this.children.remove(child);
        child.setParent(null);
    }

    private void updateChildren() {
        for (Transform child : this.children) {
            child.recalculateMatrix();
        }
    }

    public void addChangeObserver(Observer<TransformChangeEvent> observer) {
        this.changeEventManager.addObserver(observer);
    }

    public void removeChangeObserver(Observer<TransformChangeEvent> observer) {
        this.changeEventManager.removeObserver(observer);
    }

    @Override
    public GameObject gameObject() {
        return this.gameObject;
    }

    public PVector position() {
        return PVector.add(this.parent.position(), this.position);
    }

    public PVector screenPosition() {
        PVector copy = this.position.copy();
        Camera camera = Game.getInstance().getCamera();
        return Transform.translateVector(camera, copy).add(0, this.height());
    }

    public float rotation() {
        return this.parent.rotation() + this.rotation;
    }

    public PVector scale() {
        PVector parentScale = this.parent.scale();
        return new PVector(this.scale.x * parentScale.x, this.scale.y * parentScale.y,
                this.scale.z * parentScale.z);
    }

    public float yScale() {
        return this.parent.yScale() * this.scale.y;
    }

    public float height() {
        return this.parent.height() + this.height;
    }

    public float setHeight(float height) {
        this.changeEventManager
                .notify(new TransformChangeEvent(this, ChangeType.HEIGHT, this.height, height));
        this.height = height;
        return this.height;
    }

    public void setParent(Transform parent) {
        this.changeEventManager
                .notify(new TransformChangeEvent(this, ChangeType.PARENT, this.parent, parent));
        this.parent.children.remove(this);
        this.parent = parent == null ? IDENTITY : parent;
        this.recalculateMatrix();
    }

    public void setPosition(PVector position) {
        this.changeEventManager.notify(
                new TransformChangeEvent(this, ChangeType.POSITION, this.position, position));
        this.position = position;
        this.recalculateMatrix();
    }

    public void setRotation(float rotation) {
        this.changeEventManager.notify(
                new TransformChangeEvent(this, ChangeType.ROTATION, this.rotation, rotation));
        this.rotation = rotation;
        this.recalculateMatrix();
    }

    public void setScale(PVector scale) {
        this.changeEventManager
                .notify(new TransformChangeEvent(this, ChangeType.SCALE, this.scale, scale));
        this.scale = scale;
        this.recalculateMatrix();
    }

    public void setLocalPosition(PVector position) {
        this.setPosition(PVector.sub(position, this.parent.position()));
    }

    public void setLocalRotation(float rotation) {
        this.setRotation(rotation - this.parent.rotation());
    }

    public void setLocalScale(PVector scale) {
        PVector parentScale = this.parent.scale();
        PVector newScale = new PVector(Helper.safeDivide(scale.x, parentScale.x),
                Helper.safeDivide(scale.y, parentScale.y),
                Helper.safeDivide(scale.z, parentScale.z));
        this.setScale(newScale);
    }

    private void recalculateMatrix() {
        this.translationMatrix = new PMatrix2D();
        this.scaleMatrix = new PMatrix2D();

        this.scaleMatrix.scale(this.scale.x, this.scale.z);
        this.rotationMatrix.rotate(this.rotation);
        this.translationMatrix.translate(this.position.x, this.position.y);


        this.combinedMatrix = new PMatrix2D();
        this.combinedMatrix.apply(this.scaleMatrix);
        this.combinedMatrix.apply(this.translationMatrix);

        if (this.parent != null) {
            this.combinedMatrix.preApply(this.parent.getCombinedMatrix());
        }

        this.updateChildren();
    }

    public PMatrix2D getCombinedMatrix() {
        return this.combinedMatrix;
    }

    public PVector transformVertex(PVector vertex) {
        PVector transformedVertex = new PVector();
        PMatrix2D matrixCopy = this.combinedMatrix.get();
        matrixCopy.translate(this.position.x, this.position.y);
        matrixCopy.rotate((float) Math.toRadians(this.rotation));
        matrixCopy.mult(vertex, transformedVertex);
        return transformedVertex;
    }

    public static PVector translateVector(Camera camera, PVector vector) {
        PMatrix2D cameraMatrix = camera.getMatrix();
        PVector translatedVector = new PVector();
        cameraMatrix.mult(vector, translatedVector);
        return translatedVector;
    }

    public static PVector inverseTranslateVector(Camera camera, PVector vector) {
        PMatrix2D cameraMatrix = camera.getMatrix();
        cameraMatrix.invert();
        PVector translatedVector = new PVector();
        cameraMatrix.mult(vector, translatedVector);
        return translatedVector;
    }
}


