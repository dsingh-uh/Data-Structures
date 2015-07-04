public class BPlusTree<Key extends Comparable<Key>, Value> {

  Node root;
  Node head;

  private static final int MAX_KEYS_COUNT = 10; // Should be greater than one. One requires
                                               // special handling
  private static final int MAX_VALUES_COUNT = MAX_KEYS_COUNT + 1;
  private static final int RECORDS_PER_BLOCK = 4;

  public void insert(Key key, Value value) throws Exception {
    if (root == null) {
      root = new LNode();
      head = root;
    } else if(root.hasKey(key)) {
      System.err.println(key.toString() + " has aready been added. Ignoring the insert.");
      return;
    }

    root.insert(key, value);
  }

  public void snapshot() {
    System.out.println("\n\nSNAPSHOT");
    if (root == null) {
      System.out.println("Tree is empty");
    } else {
      System.out.println(count() + " records in the table");
      System.out.println(blocksCount() + " blocks");
      System.out.println("Depth of " + height());
    }
  }

  public void printTree() {
    System.out.println("\n\nALL NODES");
    if (root == null) {
      System.out.println("Tree is empty");
    } else {
      root.print();
    }
  }

  public int count() {
    return root == null? 0: root.count();
  }

  public int blocksCount() {
    return root == null? 0: root.blocksCount();
  }

  public int height() {
    return root == null? 0: root.height();
  }

  public void search(Key key) {
    final KeyValue result = root.search(key);
    if (result == null) {
      System.out.println("Key, " + key + ", not found.");
    } else {
      //System.out.println("Key: " + result.key + ", Value: " + result.value);
      System.out.println("Key: " + result.key + ", Found "); 
    }
  }

  public void update(Key key, Value value) {
    root.update(key, value);
  }

  public void list() {
    LNode cur = (LNode) head;
    while (cur != null) {
      for (int i = 0; i < cur.keysCount; ++i) {
        System.out.print(cur.keys[i] + " --> ");
      }
      cur = cur.next;
    }
    System.out.println();
  }

  public void delete(Key key) {
    root.delete(key);
  }

  private abstract class Node {
    Key[] keys;
    int keysCount;
    public abstract Partition insert(Key key, Value value);
    public abstract void print();
    public abstract KeyValue search(Key key);
    public abstract void update(Key key, Value value);
    public abstract boolean delete(Key key);
    public abstract int count();
    public abstract int blocksCount();
    public abstract int height();

    public Node() {
      keys = (Key[]) new Comparable[MAX_KEYS_COUNT + 1];
      keysCount = 0;
    }

    public boolean hasKey(Key key) {
      if (keysCount > 0) {
        for (Key k : keys) {
          if (k != null && k.equals(key)) {
            return true;
          }
        }
      }

      return false;
    }
  }

  private class INode extends Node {
    final Node[] links = new BPlusTree.Node[MAX_VALUES_COUNT + 1];

    public INode() {
      super();
    }

    @Override
    public Partition insert(Key key, Value value) {
      int i = 0;
      while (i < keysCount && key.compareTo(keys[i]) >= 0) {
        ++i;
      }

      Partition partition = links[i].insert(key, value);

      if (partition != null) {
        insert(partition);
      }

      return null;
    }

    public void insert(Partition partition) {
      if (keysCount == MAX_KEYS_COUNT) {
        insertWithoutPartition(partition);

        // split
        int mid = keysCount / 2;

        INode leftINode = new INode();
        INode rightINode = new INode();

        for (int i = 0; i < mid; ++i) {
          leftINode.insert(new Partition(keys[i], links[i], links[i+1]));
        }

        for (int i = mid + 1; i < keysCount; ++i) {
          rightINode.insert(new Partition(keys[i], links[i], links[i+1]));
        }

        keys[0] = keys[mid];
        links[0] = leftINode;
        links[1] = rightINode;

        for (int i = 1 ; i < keysCount; ++i) {
          keys[i] = null;
          links[i + 1] = null;
        }
        keysCount = 1;
      } else {
        insertWithoutPartition(partition);
      }
    }

    private void insertWithoutPartition(Partition partition) {
      if (keysCount > 0) {
        int i = 0;
        while (i < keysCount && partition.key.compareTo(keys[i]) >= 0) {
          ++i;
        }
        for (int iter = keysCount; iter > i; --iter) {
          keys[iter] = keys[iter - 1];
        }
        keys[i] = partition.key;

        for (int iter = keysCount + 1; iter > i + 1; --iter) {
          links[iter] = links[iter - 1];
        }
        links[i + 1] = partition.nextNode;
      } else {
        keys[0] = partition.key;
        links[0] = partition.prevNode;
        links[1] = partition.nextNode;
      }
      ++keysCount;
    }

    @Override
    public void print() {
      System.out.println("\nINODE:");
      System.out.print("  keys: ");
      for (int i = 0; i < keysCount; ++i) {
        System.out.print(keys[i] + "; ");
      }
      System.out.println();
      System.out.print("  links: ");
      for (int i = 0; i < keysCount + 1; ++i) {
        System.out.print(links[i].keys[0] + "; ");
      }
      System.out.println();

      for (Node link: links) {
        if (link != null) {
          link.print();
        }
      }
    }

    @Override
    public KeyValue search(Key key) {
      int i = 0;
      if (keysCount > 0) {
        while (i < keysCount && key.compareTo(keys[i]) >= 0) {
          ++i;
        }
      }

      if (i >= keysCount) --i;

      if (key.compareTo(keys[i]) < 0) {
        return links[i].search(key);
      } else {
        return links[i+1].search(key);
      }
    }

    @Override
    public void update(Key key, Value value) {
      int i = 0;
      if (keysCount > 0) {
        while (i < keysCount && key.compareTo(keys[i]) >= 0) {
          ++i;
        }
      }

      if (i >= keysCount) --i;

      if (key.compareTo(keys[i]) < 0) {
        links[i].update(key, value);
      } else {
        links[i+1].update(key, value);
      }
    }

    @Override
    public boolean delete(Key key) {
      int i = 0;
      if (keysCount > 0) {
        while (i < keysCount && key.compareTo(keys[i]) >= 0) {
          ++i;
        }
      }

      if (i >= keysCount) --i;

      int linkToDel = key.compareTo(keys[i]) < 0 ? i : (i+1);
      boolean delLink = links[linkToDel].delete(key);
      //System.out.println("linkToDel: " + linkToDel);
      //System.out.println("delLink: " + delLink);
      //System.out.println("keys[0]: " + keys[0]);

      if (delLink) {
        if (links[linkToDel] instanceof BPlusTree.INode) {
          links[linkToDel] = ((INode)links[linkToDel]).links[0];
        } else {
          if (keysCount == 1) {
            if (linkToDel == 0) {
              links[0] = links[1];
            }
            keys[0] = null;
            links[1] = null;
            --keysCount;
          } else {
            if (linkToDel == 0) {
              links[0] = links[1];
              if (1 != keysCount) {
                for (int iter = 1; iter < keysCount - 1; ++iter) {
                  keys[iter] = keys[iter + 1];
                  links[iter + 1] = links[iter + 2];
                }
                keys[keysCount - 1] = null;
                links[keysCount] = null;
                --keysCount;
              }
            } else {
              if (linkToDel != keysCount) {
                for (int iter = linkToDel; iter < keysCount; ++iter) {
                  keys[iter - 1] = keys[iter];
                  links[iter] = links[iter + 1];
                }
              }
              keys[keysCount - 1] = null;
              links[keysCount] = null;
              --keysCount;
            }
          }
        }
      }
      else{
        System.out.println("Deletion not possible, key not found");
      }

      return keysCount == 0;
    }

    @Override
    public int count() {
      int count = 0;
      for (int i = 0; i < keysCount + 1; ++i) {
        if (links[i] != null) {
          count += links[i].count();
        }
      }

      return count;
    }

    @Override
    public int blocksCount() {
      int blocksCount = 0;
      for (int i = 0; i < keysCount + 1; ++i) {
        if (links[i] != null) {
          blocksCount += links[i].blocksCount();
        }
      }

      return blocksCount;
    }

    @Override
    public int height() {
      int height = 0;
      for (int i = 0; i < keysCount + 1; ++i) {
        final int linkHeight = links[i].height();
        if (linkHeight > height) {
          height = linkHeight;
        }
      }

      return height + 1;
    }
  }

  private class LNode extends Node {
    Value[] values;
    LNode next;
    LNode prev;

    public LNode() {
      super();
      values = (Value[]) new Object[MAX_VALUES_COUNT + 1];
      next = null;
      prev = null;
    }

    @Override
    public Partition insert(Key key, Value value) {
      if (keysCount == MAX_KEYS_COUNT) {
        //insertWithoutPartition(key, value);

        // Split LNode into two LNodes and create an INode
        int mid = keysCount / 2;
        LNode newLNode = new LNode();
        newLNode.next = next;
        newLNode.prev = this;
        next = newLNode;


        for(int i = mid; i < MAX_KEYS_COUNT; ++i) {
          newLNode.insert(keys[i], values[i]);
          keys[i] = null;
          values[i] = null;
          --keysCount;
        }

        if (key.compareTo(newLNode.keys[0]) < 0) {
          insert(key, value);
        } else {
          newLNode.insert(key, value);
        }

        if (head == this) {

        }

        if (root == this) {
          root = new INode();
          ((INode) root).insert(new Partition(newLNode.keys[0], this, newLNode));
          return null;
        } else {
          return new Partition(newLNode.keys[0], this, newLNode);
        }
      } else {
        return insertWithoutPartition(key, value);
      }
    }

    private Partition insertWithoutPartition(Key key, Value value) {
      int cur = 0;
      for (; cur < keysCount; ++cur) {
        //log("\nkeysCount: " + keysCount);
        //log("cur: " + cur);
        //log("key: " + key);
        //log("keys[cur]: " + keys[cur]);
        //log("");
        if (key.compareTo(keys[cur]) < 0) {
          break;
        }
      }

      if (cur < keysCount) {
        for (int i = keysCount; i > cur; --i) {
          keys[i] = keys[i - 1];
        }

        for (int i = keysCount; i > cur; --i) {
          values[i] = values[i - 1];
        }
      }
      keys[cur] = key;
      values[cur] = value;

      ++keysCount;
      return null;
    }

    @Override
    public void print() {
      System.out.println("LNODE:");
      for (int i = 0; i < keysCount; ++i) {
        log("  key: " + keys[i] + ", value: " + values[i]);
      }

      log(" NEXT: " + (next == null ? "null" : next.keys[0]));
    }

    @Override
    public KeyValue search(Key key) {
      for (int cur = 0; cur < keysCount; ++cur) {
        if (key.compareTo(keys[cur]) == 0) {
          return new KeyValue(keys[cur], values[cur]);
        }
      }

      return null;
    }

    @Override
    public void update(Key key, Value value) {
      for (int cur = 0; cur < keysCount; ++cur) {
        if (key.compareTo(keys[cur]) == 0) {
          values[cur] = value;
        }
      }
    }

    @Override
    public boolean delete(Key key) {
      for (int cur = 0; cur < keysCount; ++cur) {
        if (key.compareTo(keys[cur]) == 0) {
          System.out.println("Deleting " + keys[cur]);
          for (int i = cur; i < keysCount - 1; ++i) {
            keys[cur] = keys[cur + 1];
            values[cur] = values[cur + 1];
          }
          keys[keysCount - 1] = null;
          values[keysCount - 1] = null;
          --keysCount;
        }
      }

      if (keysCount <= 0) {
        // delete
        if (next != null) {
          next.prev = prev;
        }
        if (prev != null) {
          prev.next = next;
        }
      }

      return keysCount == 0;
    }

    @Override
    public int count() {
      return keysCount;
    }

    @Override
    public int blocksCount() {
      return (int)Math.ceil((double)keysCount / (double)RECORDS_PER_BLOCK);
    }

    @Override
    public int height() {
      return 1;
    }

    private void log(String str) {
      System.out.println(str);
    }
  }

  private class Partition {
    Key key;
    Node prevNode;
    Node nextNode;

    public Partition(Key k, Node pn, Node nn) {
      key = k;
      prevNode = pn;
      nextNode = nn;
    }

    @Override
    public String toString() {
      return "Partition [" + key + ", " + prevNode.keys[0] + ", " + nextNode.keys[0] + "]";
    }
  }

  private class KeyValue {
    Key key;
    Value value;

    public KeyValue (Key key, Value value) {
      this.key = key;
      this.value = value;
    }
  }
}
